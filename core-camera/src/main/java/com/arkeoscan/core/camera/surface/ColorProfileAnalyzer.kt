package com.arkeoscan.core.camera.surface

import android.graphics.Bitmap
import com.arkeoscan.core.common.model.ColorProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bir fotoğrafın renk istatistiklerini (ortalama RGB + parlaklık varyansı)
 * çıkarır. Performans için bitmap'i küçük bir örnekleme ızgarasına indirger
 * (her piksele bakmak yerine, sabit sayıda örnek noktası taranır).
 */
@Singleton
class ColorProfileAnalyzer @Inject constructor() {

    /**
     * @param sampleGridSize Görüntüyü sampleGridSize x sampleGridSize ızgarasına
     *   bölüp her hücrenin merkezinden örnek alır. 32x32 = 1024 örnek, tipik bir
     *   saha fotoğrafı için (4000x3000 gibi) hızlı ve yeterince temsili.
     */
    fun analyze(bitmap: Bitmap, sampleGridSize: Int = 32): ColorProfile {
        val width = bitmap.width
        val height = bitmap.height
        if (width == 0 || height == 0) {
            return ColorProfile(0f, 0f, 0f, 0f)
        }

        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        val brightnessValues = ArrayList<Float>(sampleGridSize * sampleGridSize)

        val stepX = (width / sampleGridSize).coerceAtLeast(1)
        val stepY = (height / sampleGridSize).coerceAtLeast(1)

        var sampleCount = 0
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                sumR += r
                sumG += g
                sumB += b

                // Perceptual luminance (ITU-R BT.601 yaklaşıklığı)
                val brightness = 0.299f * r + 0.587f * g + 0.114f * b
                brightnessValues.add(brightness)

                sampleCount++
                x += stepX
            }
            y += stepY
        }

        if (sampleCount == 0) {
            return ColorProfile(0f, 0f, 0f, 0f)
        }

        val meanR = sumR.toFloat() / sampleCount
        val meanG = sumG.toFloat() / sampleCount
        val meanB = sumB.toFloat() / sampleCount

        val meanBrightness = brightnessValues.average().toFloat()
        val variance = brightnessValues.sumOf { (it - meanBrightness).toDouble().let { d -> d * d } } / sampleCount

        return ColorProfile(
            meanRed = meanR,
            meanGreen = meanG,
            meanBlue = meanB,
            brightnessVariance = variance.toFloat()
        )
    }
}
