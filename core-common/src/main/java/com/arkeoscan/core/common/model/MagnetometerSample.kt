package com.arkeoscan.core.common.model

import kotlin.math.sqrt

/**
 * Tek bir manyetometre örneği. TYPE_MAGNETIC_FIELD sensöründen gelir.
 *
 * @property x,y,z Manyetik alan bileşenleri (µT, mikroTesla)
 * @property totalMicroTesla |B| = sqrt(x²+y²+z²)
 * @property timestampMillis Epoch milisaniye
 * @property latitude/longitude Örnek alındığı anki konum (Walk Scan ile eşleştirme için)
 */
data class MagnetometerSample(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestampMillis: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val totalMicroTesla: Float
        get() = sqrt(x * x + y * y + z * z)
}

/**
 * Kalibrasyon sonucu hesaplanan taban (baseline) değer ve istatistikler.
 */
data class MagnetometerBaseline(
    val meanMicroTesla: Float,
    val stdDevMicroTesla: Float,
    val minMicroTesla: Float,
    val maxMicroTesla: Float,
    val sampleCount: Int
) {
    /**
     * Verilen bir okumanın baseline'dan kaç sigma saptığını hesaplar.
     */
    fun deviationSigma(value: Float): Float {
        if (stdDevMicroTesla <= 0f) return 0f
        return (value - meanMicroTesla) / stdDevMicroTesla
    }
}
