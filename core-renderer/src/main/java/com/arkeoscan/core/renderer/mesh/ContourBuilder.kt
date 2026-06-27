package com.arkeoscan.core.renderer.mesh

import com.arkeoscan.core.common.model.InterpolatedGrid
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Marching Squares algoritması ile InterpolatedGrid üzerinde izoline (contour)
 * segmentleri üretir. "Surfer benzeri contour üret, izolines oluştur, renkli contour"
 * gereksinimi burada karşılanır.
 *
 * Her contour seviyesi (isovalue) için, grid'in 2x2 hücre bloklarından oluşan her
 * "kare"de hücre köşe değerlerinin isovalue'yu nerede kestiği lineer interpolasyonla
 * bulunur ve bu kesişim noktaları birleştirilerek çizgi segmentleri elde edilir.
 *
 * NOT: Bu implementasyon hücre DEĞERLERİNİ (cell-centered) köşe değeri gibi kullanır;
 * grid çözünürlüğü saha taraması ölçeğinde (0.5-2m) yeterince sık olduğundan bu
 * yaklaşıklık görsel olarak Surfer/Golden Software çıktılarına yakın sonuç verir.
 */
object ContourBuilder {

    data class ContourLine(
        val isovalue: Float,
        /** Her segment 2 nokta (x1,y1)-(x2,y2) olarak ardışık saklanır. */
        val segments: List<FloatArray>
    )

    fun buildContours(grid: InterpolatedGrid, levels: Int = 8): List<ContourLine> {
        if (grid.rows < 2 || grid.cols < 2) return emptyList()

        val range = grid.maxValue - grid.minValue
        if (range <= 0f) return emptyList()

        val step = range / (levels + 1)
        val isovalues = (1..levels).map { grid.minValue + it * step }

        return isovalues.map { iso -> ContourLine(iso, marchSquares(grid, iso)) }
    }

    private fun marchSquares(grid: InterpolatedGrid, isovalue: Float): List<FloatArray> {
        val segments = mutableListOf<FloatArray>()

        for (row in 0 until grid.rows - 1) {
            for (col in 0 until grid.cols - 1) {
                val c00 = grid.cellAt(row, col) ?: continue
                val c10 = grid.cellAt(row, col + 1) ?: continue
                val c01 = grid.cellAt(row + 1, col) ?: continue
                val c11 = grid.cellAt(row + 1, col + 1) ?: continue

                // Polygon dışındaki herhangi bir köşe varsa bu kareyi atla
                // ("Polygon dışı görünmesin" gereksinimi contour için de uygulanır).
                if (!c00.isInsidePolygon || !c10.isInsidePolygon ||
                    !c01.isInsidePolygon || !c11.isInsidePolygon
                ) continue

                val v00 = c00.value
                val v10 = c10.value
                val v01 = c01.value
                val v11 = c11.value

                // Marching squares case index (4-bit): hangi köşeler isovalue üstünde
                var caseIndex = 0
                if (v00 > isovalue) caseIndex = caseIndex or 1
                if (v10 > isovalue) caseIndex = caseIndex or 2
                if (v11 > isovalue) caseIndex = caseIndex or 4
                if (v01 > isovalue) caseIndex = caseIndex or 8

                if (caseIndex == 0 || caseIndex == 15) continue // tamamen altında/üstünde, çizgi yok

                // Kenar orta noktalarını lineer interpolasyonla bul
                fun lerpEdge(
                    xa: Double, ya: Double, va: Float,
                    xb: Double, yb: Double, vb: Float
                ): FloatArray {
                    val t = if (vb != va) (isovalue - va) / (vb - va) else 0.5f
                    val x = xa + t * (xb - xa)
                    val y = ya + t * (yb - ya)
                    return floatArrayOf(x.toFloat(), y.toFloat())
                }

                val bottom = lazy { lerpEdge(c00.localX, c00.localY, v00, c10.localX, c10.localY, v10) }
                val right = lazy { lerpEdge(c10.localX, c10.localY, v10, c11.localX, c11.localY, v11) }
                val top = lazy { lerpEdge(c01.localX, c01.localY, v01, c11.localX, c11.localY, v11) }
                val left = lazy { lerpEdge(c00.localX, c00.localY, v00, c01.localX, c01.localY, v01) }

                // Standart marching squares kenar tablosu (16 case, simetrik 1-7/9-15 ve
                // ortak çapraz durumlar (5,10) için saddle-point ortalama kullanılır).
                val edgePairs: List<Pair<Lazy<FloatArray>, Lazy<FloatArray>>> = when (caseIndex) {
                    1, 14 -> listOf(left to bottom)
                    2, 13 -> listOf(bottom to right)
                    3, 12 -> listOf(left to right)
                    4, 11 -> listOf(right to top)
                    6, 9 -> listOf(bottom to top)
                    7, 8 -> listOf(left to top)
                    5 -> listOf(left to bottom, right to top) // saddle
                    10 -> listOf(left to top, bottom to right) // saddle
                    else -> emptyList()
                }

                for ((p1, p2) in edgePairs) {
                    val a = p1.value
                    val b = p2.value
                    segments.add(floatArrayOf(a[0], a[1], b[0], b[1]))
                }
            }
        }
        return segments
    }

    fun toFloatBuffer(segments: List<FloatArray>): Pair<FloatBuffer, Int> {
        val flat = ArrayList<Float>(segments.size * 6)
        for (seg in segments) {
            // Z=0.01 (heatmap düzleminin hemen üstünde, z-fighting önlemek için)
            flat.add(seg[0]); flat.add(seg[1]); flat.add(0.01f)
            flat.add(seg[2]); flat.add(seg[3]); flat.add(0.01f)
        }
        val buffer = ByteBuffer.allocateDirect(flat.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(flat.toFloatArray())
        buffer.position(0)
        return buffer to (flat.size / 3)
    }
}
