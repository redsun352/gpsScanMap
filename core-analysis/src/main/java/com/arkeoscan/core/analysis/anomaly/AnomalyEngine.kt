package com.arkeoscan.core.analysis.anomaly

import com.arkeoscan.core.common.model.AnomalyResult
import com.arkeoscan.core.common.model.AnomalyShape
import com.arkeoscan.core.common.model.GridCell
import com.arkeoscan.core.common.model.InterpolatedGrid
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anomali Motoru.
 *
 * Pipeline:
 * 1. Manyetik sapma: her hücrenin grid ortalamasından (mean) kaç sigma saptığını hesaplar.
 * 2. Eşik üstü hücreleri flood-fill ile kümeler (cluster) halinde gruplar -> "devamlılık"
 *    (continuity) ve "komşuluk" (neighborhood) buradan çıkar.
 * 3. Her küme için: alan (m²), şekil sınıflandırması, güven puanı hesaplanır.
 *
 * ÖNEMLİ: Bu motor SADECE "manyetik anomali" / "yüzey anomalisi" / "incelemeye değer
 * bölge" etiketleri üretir. Yeraltında oda/tünel/lahit gibi yapıların kesin varlığına
 * dair hiçbir çıkarım üretmez ve üretmemelidir (AnomalyResult.DISCLAIMER'a bakınız).
 */
@Singleton
class AnomalyEngine @Inject constructor() {

    fun detect(
        sessionId: Long,
        grid: InterpolatedGrid,
        sigmaThreshold: Float = 2.0f
    ): List<AnomalyResult> {
        val insideCells = grid.cells.filter { it.isInsidePolygon }
        if (insideCells.isEmpty()) return emptyList()

        val mean = grid.meanValue
        val stdDev = computeStdDev(insideCells.map { it.value }, mean)
        if (stdDev <= 0f) return emptyList()

        // Sigma eşiğini geçen hücreleri işaretle (mutlak sapma; pozitif ve negatif anomaliler)
        val cellSigma = HashMap<Pair<Int, Int>, Float>()
        for (cell in insideCells) {
            val sigma = (cell.value - mean) / stdDev
            if (Math.abs(sigma) >= sigmaThreshold) {
                cellSigma[cell.row to cell.col] = sigma
            }
        }
        if (cellSigma.isEmpty()) return emptyList()

        val clusters = floodFillClusters(cellSigma.keys, grid)

        return clusters.mapNotNull { clusterCells ->
            buildAnomalyResult(sessionId, clusterCells, cellSigma, grid, mean, stdDev)
        }.sortedByDescending { it.confidenceScore }
    }

    private fun computeStdDev(values: List<Float>, mean: Float): Float {
        if (values.isEmpty()) return 0f
        val variance = values.sumOf { (it - mean).toDouble().let { d -> d * d } } / values.size
        return Math.sqrt(variance).toFloat()
    }

    /**
     * 4-komşuluk (yukarı/aşağı/sol/sağ) flood-fill ile birbirine bitişik eşik-üstü
     * hücreleri tek bir kümeye (cluster) toplar. "Devamlılık" (continuity) skoru
     * bir kümenin ne kadar kompakt/bitişik olduğunu ölçmenin temelini oluşturur.
     */
    private fun floodFillClusters(
        markedCells: Set<Pair<Int, Int>>,
        grid: InterpolatedGrid
    ): List<List<Pair<Int, Int>>> {
        val visited = HashSet<Pair<Int, Int>>()
        val clusters = mutableListOf<List<Pair<Int, Int>>>()

        for (start in markedCells) {
            if (start in visited) continue

            val clusterCells = mutableListOf<Pair<Int, Int>>()
            val queue = ArrayDeque<Pair<Int, Int>>()
            queue.add(start)
            visited.add(start)

            while (queue.isNotEmpty()) {
                val (r, c) = queue.removeFirst()
                clusterCells.add(r to c)

                val neighbors = listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1)
                for (n in neighbors) {
                    if (n in markedCells && n !in visited) {
                        visited.add(n)
                        queue.add(n)
                    }
                }
            }
            clusters.add(clusterCells)
        }
        return clusters
    }

    private fun buildAnomalyResult(
        sessionId: Long,
        clusterCellCoords: List<Pair<Int, Int>>,
        cellSigma: Map<Pair<Int, Int>, Float>,
        grid: InterpolatedGrid,
        gridMean: Float,
        gridStdDev: Float
    ): AnomalyResult? {
        if (clusterCellCoords.isEmpty()) return null

        val clusterGridCells = clusterCellCoords.mapNotNull { (r, c) -> grid.cellAt(r, c) }
        if (clusterGridCells.isEmpty()) return null

        // Merkez: kümedeki hücrelerin ağırlık merkezi (lat/lon ortalaması)
        val centerLat = clusterGridCells.map { it.latitude }.average()
        val centerLon = clusterGridCells.map { it.longitude }.average()

        // Sapma: kümenin ortalama sigma'sı (en güçlü anomali değeri raporlanır)
        val avgSigma = clusterCellCoords.mapNotNull { cellSigma[it] }.map { Math.abs(it) }.average().toFloat()

        // Alan: hücre sayısı * hücre alanı
        val cellAreaSqM = grid.resolutionMeters * grid.resolutionMeters
        val areaSqM = clusterGridCells.size * cellAreaSqM

        // Devamlılık (continuity): kümenin "doluluk oranı" — bounding box alanına göre
        // kümenin kapladığı hücre oranı. 1.0 = mükemmel dolu dikdörtgen, düşük = dağınık/seyrek.
        val continuity = computeContinuity(clusterCellCoords)

        // Komşuluk (neighborhood): kümenin grid genelindeki diğer anomali kümelerine
        // olan yakınlığı/izolasyonu yerine, burada kümenin kendi içindeki hücreler arası
        // ortalama bitişiklik yoğunluğunu temsil eder (her hücrenin kümeli komşu sayısı / 4).
        val neighborhoodScore = computeNeighborhoodScore(clusterCellCoords)

        // Şekil analizi: bounding box en-boy oranı ve doluluk oranına göre basit sınıflandırma
        val shape = classifyShape(clusterCellCoords, continuity)

        // Güven puanı: sigma büyüklüğü, devamlılık, komşuluk ve küme büyüklüğünün
        // ağırlıklı birleşimi. 0-1 aralığına sıkıştırılır.
        val sizeScore = (clusterGridCells.size / 25f).coerceIn(0f, 1f) // 25 hücre = "yeterince büyük" referansı
        val sigmaScore = ((avgSigma - 2f) / 4f).coerceIn(0f, 1f) // 2 sigma=0, 6 sigma=1 doygunluk
        val confidence = (0.35f * sigmaScore + 0.25f * continuity + 0.20f * neighborhoodScore + 0.20f * sizeScore)
            .coerceIn(0f, 1f)

        return AnomalyResult(
            sessionId = sessionId,
            centerLatitude = centerLat,
            centerLongitude = centerLon,
            deviationSigma = avgSigma,
            areaSquareMeters = areaSqM,
            shape = shape,
            continuityScore = continuity,
            neighborhoodScore = neighborhoodScore,
            confidenceScore = confidence,
            label = "Manyetik anomali",
            cellRowIndices = clusterCellCoords.map { it.first },
            cellColIndices = clusterCellCoords.map { it.second }
        )
    }

    private fun computeContinuity(cells: List<Pair<Int, Int>>): Float {
        val minRow = cells.minOf { it.first }
        val maxRow = cells.maxOf { it.first }
        val minCol = cells.minOf { it.second }
        val maxCol = cells.maxOf { it.second }

        val bboxArea = (maxRow - minRow + 1) * (maxCol - minCol + 1)
        if (bboxArea <= 0) return 0f

        return (cells.size.toFloat() / bboxArea.toFloat()).coerceIn(0f, 1f)
    }

    private fun computeNeighborhoodScore(cells: List<Pair<Int, Int>>): Float {
        val cellSet = cells.toHashSet()
        if (cellSet.isEmpty()) return 0f

        var totalNeighborFraction = 0f
        for ((r, c) in cellSet) {
            val neighbors = listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1)
            val presentCount = neighbors.count { it in cellSet }
            totalNeighborFraction += presentCount / 4f
        }
        return (totalNeighborFraction / cellSet.size).coerceIn(0f, 1f)
    }

    /**
     * Basit geometrik sınıflandırma:
     * - Doluluk oranı yüksek + en-boy oranı ~1 -> CIRCULAR/RECTANGULAR (dairesel/dörtgen ayrımı
     *   piksel grid çözünürlüğünde güvenilir yapılamaz, bu yüzden ikisi REKTANGULAR olarak
     *   gruplanır; gerçek bir şekil tanıma için kontur eğriliği analizi gerekir).
     * - En-boy oranı yüksek (uzun-ince) -> LINEAR (örn. duvar kalıntısı, hat şeklinde anomali).
     * - Doluluk oranı düşük ve dağınık -> IRREGULAR.
     */
    private fun classifyShape(cells: List<Pair<Int, Int>>, continuity: Float): AnomalyShape {
        val minRow = cells.minOf { it.first }
        val maxRow = cells.maxOf { it.first }
        val minCol = cells.minOf { it.second }
        val maxCol = cells.maxOf { it.second }

        val height = (maxRow - minRow + 1).toFloat()
        val width = (maxCol - minCol + 1).toFloat()
        val aspectRatio = Math.max(height, width) / Math.max(1f, Math.min(height, width))

        return when {
            continuity < 0.35f -> AnomalyShape.IRREGULAR
            aspectRatio >= 3f -> AnomalyShape.LINEAR
            aspectRatio in 0.8f..1.3f && continuity >= 0.6f -> AnomalyShape.CIRCULAR
            continuity >= 0.5f -> AnomalyShape.RECTANGULAR
            else -> AnomalyShape.UNKNOWN
        }
    }
}
