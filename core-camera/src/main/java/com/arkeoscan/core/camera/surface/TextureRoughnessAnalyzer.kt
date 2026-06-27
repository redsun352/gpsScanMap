package com.arkeoscan.core.camera.surface

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Basit gradyan-tabanlı doku pürüzlülüğü tahmincisi.
 *
 * Mantık: bir görüntü bölgesindeki komşu piksel parlaklık farklarının
 * büyüklüğü, o yüzeyin "pürüzlülüğünü" (taş, moloz, düzensiz toprak) ya da
 * "düzlüğünü" (sıkışmış toprak, düzgün zemin, beton) gösterir. Bu, tam bir
 * Sobel/Canny kenar tespiti değildir — yatay+dikey komşu piksel farklarının
 * ortalama mutlak değeri alınır (basitleştirilmiş gradyan büyüklüğü).
 *
 * Arkeolojik mantık: gömülü duvar/taban gibi yapıların üzerindeki toprak,
 * çevresine göre farklı sıkışma/taş yoğunluğu gösterebilir (buna "soil mark"
 * denir) — bu da yüzey dokusunda gözle veya fotoğrafta fark edilebilir bir
 * kontrast oluşturabilir. Bu sınıf SADECE bu görsel dokuyu sayısallaştırır;
 * yapının kendisini tespit etmez.
 */
@Singleton
class TextureRoughnessAnalyzer @Inject constructor() {

    /**
     * @return 0'a yakın değerler düz/homojen yüzey, yüksek değerler
     *   (tipik aralık 0-80 civarı, 8-bit parlaklık farkı ölçeğinde) pürüzlü/
     *   düzensiz yüzey demektir.
     */
    fun analyze(bitmap: Bitmap, sampleGridSize: Int = 48): Float {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 2 || height < 2) return 0f

        val stepX = (width / sampleGridSize).coerceAtLeast(1)
        val stepY = (height / sampleGridSize).coerceAtLeast(1)

        var sumGradient = 0.0
        var sampleCount = 0

        var y = stepY
        while (y < height - 1) {
            var x = stepX
            while (x < width - 1) {
                val center = luminance(bitmap.getPixel(x, y))
                val right = luminance(bitmap.getPixel(x + 1, y))
                val below = luminance(bitmap.getPixel(x, y + 1))

                val gradX = Math.abs(center - right)
                val gradY = Math.abs(center - below)
                sumGradient += (gradX + gradY) / 2.0

                sampleCount++
                x += stepX
            }
            y += stepY
        }

        return if (sampleCount > 0) (sumGradient / sampleCount).toFloat() else 0f
    }

    private fun luminance(pixel: Int): Float {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return 0.299f * r + 0.587f * g + 0.114f * b
    }
}
