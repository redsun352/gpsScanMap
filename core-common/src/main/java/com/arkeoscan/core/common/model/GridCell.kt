package com.arkeoscan.core.common.model

/**
 * Interpolasyon (IDW) sonrası üretilen tek bir grid hücresi.
 * localX/localY, polygon'un ilk noktasına göre yerel metre koordinatıdır.
 */
data class GridCell(
    val row: Int,
    val col: Int,
    val localX: Double,
    val localY: Double,
    val latitude: Double,
    val longitude: Double,
    val value: Float,
    val isInsidePolygon: Boolean,
    val confidence: Float = 1f
)

/**
 * Tüm grid hücrelerini ve metadata'sını taşıyan kapsayıcı.
 */
data class InterpolatedGrid(
    val cells: List<GridCell>,
    val rows: Int,
    val cols: Int,
    val resolutionMeters: Double,
    val minValue: Float,
    val maxValue: Float,
    val meanValue: Float
) {
    fun cellAt(row: Int, col: Int): GridCell? = cells.getOrNull(row * cols + col)
}

/**
 * Şekil sınıflandırması (Anomaly Engine'in shape analysis çıktısı).
 */
enum class AnomalyShape {
    CIRCULAR,
    LINEAR,
    RECTANGULAR,
    IRREGULAR,
    UNKNOWN
}

/**
 * Anomali motorunun bir kümeden (cluster) ürettiği nihai değerlendirme.
 *
 * NOT: Bu sınıf kasıtlı olarak yalnızca "manyetik anomali" / "yüzey anomalisi" /
 * "incelemeye değer bölge" terimleriyle raporlama yapar. Yeraltında oda, tünel,
 * lahit vb. yapıların kesin varlığına dair bir alan İÇERMEZ ve böyle bir çıkarım
 * üretmemelidir. Bilimsel dürüstlük ilkesi: ham sensör verisinden kesin yapısal
 * iddialara sıçramak yapılmaz.
 */
data class AnomalyResult(
    val id: Long = 0,
    val sessionId: Long,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val deviationSigma: Float,
    val areaSquareMeters: Double,
    val shape: AnomalyShape,
    val continuityScore: Float,
    val neighborhoodScore: Float,
    val confidenceScore: Float,
    val label: String = "Manyetik anomali",
    val cellRowIndices: List<Int> = emptyList(),
    val cellColIndices: List<Int> = emptyList()
) {
    companion object {
        const val DISCLAIMER =
            "Bu sonuç yalnızca manyetik/yüzey anomalisini gösterir; " +
                "yeraltında oda, tünel, lahit veya benzeri bir yapının kesin " +
                "varlığını kanıtlamaz. İncelemeye değer bölge olarak değerlendirilmelidir."
    }
}
