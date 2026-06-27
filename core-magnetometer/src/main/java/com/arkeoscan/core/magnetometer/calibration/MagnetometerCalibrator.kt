package com.arkeoscan.core.magnetometer.calibration

import com.arkeoscan.core.common.model.MagnetometerBaseline
import com.arkeoscan.core.common.model.MagnetometerSample
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Kalibrasyon ekranı mantığı: kullanıcı cihazı sakin tutarken (veya 8 şeklinde
 * hareket ettirirken) toplanan örneklerden baseline (taban) ve istatistikleri çıkarır.
 *
 * Ayrıca düşük gecikmeli, kayan pencereli bir "peak-to-peak gürültü tabanı" hesaplayıcı
 * sağlar (16 örneklik pencere, sahada gerçek zamanlı gürültü/sapma ayrımı için kullanılır).
 */
@Singleton
class MagnetometerCalibrator @Inject constructor() {

    /**
     * Sabit/kalibrasyon süresinde toplanan örneklerden taban istatistiklerini hesaplar.
     */
    fun computeBaseline(samples: List<MagnetometerSample>): MagnetometerBaseline? {
        if (samples.isEmpty()) return null

        val totals = samples.map { it.totalMicroTesla }
        val mean = totals.average().toFloat()
        val variance = totals.sumOf { (it - mean).toDouble().let { d -> d * d } } / totals.size
        val stdDev = sqrt(variance).toFloat()

        return MagnetometerBaseline(
            meanMicroTesla = mean,
            stdDevMicroTesla = stdDev,
            minMicroTesla = totals.min(),
            maxMicroTesla = totals.max(),
            sampleCount = samples.size
        )
    }

    /**
     * Kayan pencere (varsayılan 16 örnek) üzerinden peak-to-peak gürültü tabanını hesaplar.
     * Thuban Lodestar tarzı dedektörlerde kullanılan yaklaşım: ardışık N örneğin
     * max-min farkı, "şu anki ortamın gürültü seviyesi" olarak kabul edilir ve
     * deadzone filtresi bu değere göre dinamik ayarlanabilir.
     */
    fun peakToPeakNoiseFloor(recentSamples: List<MagnetometerSample>, windowSize: Int = 16): Float {
        if (recentSamples.isEmpty()) return 0f
        val window = recentSamples.takeLast(windowSize)
        val totals = window.map { it.totalMicroTesla }
        return (totals.max() - totals.min())
    }

    /**
     * Deadzone filtresi: gürültü tabanının altında kalan sapmaları sıfırlar.
     * Bu, sahada düşük seviyeli sensör titremesinin sahte anomali olarak
     * raporlanmasını engeller.
     */
    fun applyDeadzone(deviationMicroTesla: Float, noiseFloor: Float, marginFactor: Float = 1.5f): Float {
        val threshold = noiseFloor * marginFactor
        return if (Math.abs(deviationMicroTesla) < threshold) 0f else deviationMicroTesla
    }

    /**
     * Logaritmik kazanç eğrisi: ham sapmayı (µT) görsel/duyusal olarak daha
     * okunabilir bir 0-1 yoğunluk skalasına sıkıştırır. Büyük sapmalarda
     * doygunlaşma sağlar (heatmap renk skalasında aşırı kontrast oluşmasını önler).
     */
    fun logarithmicGain(deviationMicroTesla: Float, scaleFactor: Float = 10f): Float {
        val magnitude = Math.abs(deviationMicroTesla)
        if (magnitude <= 0f) return 0f
        val gained = (Math.log10((magnitude / scaleFactor) + 1.0)).toFloat()
        return gained.coerceIn(0f, 1f) * Math.signum(deviationMicroTesla)
    }
}
