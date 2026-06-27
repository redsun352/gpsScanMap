package com.arkeoscan.core.camera.surface

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VARI (Visible Atmospherically Resistant Index) hesaplayıcı.
 *
 * VARI = (Green - Red) / (Green + Red - Blue)
 *
 * Gitelson ve ark. (2002), "Vegetation and Soil Lines in Visible Spectral
 * Space" makalesinde tanımlanan, standart RGB kameralarla (multispektral/NIR
 * sensör gerektirmeden) bitki örtüsü yoğunluğunu tahmin etmek için kullanılan
 * bilinen bir indekstir. Aydınlatma farklılıklarına NDVI'dan daha dayanıklıdır
 * (mavi kanalın çıkarılması atmosferik/aydınlatma etkisini bir ölçüde söner).
 *
 * Arkeolojik saha taramasında kullanım mantığı: gömülü yapılar (duvar, taban,
 * dolgu farkı) genellikle üstlerindeki toprak nemini ve besin profilini
 * değiştirir, bu da bitki örtüsünün o bölgede farklı (genelde daha seyrek/
 * sararmış ya da tam tersi daha yoğun) büyümesine yol açabilir — buna
 * arkeolojide "crop mark" (ekin izi) denir ve havadan/yer fotoğraflarıyla
 * gözlemlenen, kabul görmüş bir yüzeysel keşif tekniğidir. VARI burada bu
 * türden bir kontrastı sayısallaştırmak için kullanılır; KESİN bir yapı
 * tespiti DEĞİLDİR.
 *
 * Çıkış aralığı tipik olarak [-1, 1]: negatif/sıfıra yakın değerler çıplak
 * toprak/taş, pozitif ve yüksek değerler yoğun yeşil bitki örtüsünü işaret eder.
 */
@Singleton
class VegetationIndexEstimator @Inject constructor() {

    fun estimate(bitmap: Bitmap, sampleGridSize: Int = 32): Float {
        val width = bitmap.width
        val height = bitmap.height
        if (width == 0 || height == 0) return 0f

        val stepX = (width / sampleGridSize).coerceAtLeast(1)
        val stepY = (height / sampleGridSize).coerceAtLeast(1)

        var sumVari = 0.0
        var sampleCount = 0

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val pixel = bitmap.getPixel(x, y)
                val r = ((pixel shr 16) and 0xFF).toFloat()
                val g = ((pixel shr 8) and 0xFF).toFloat()
                val b = (pixel and 0xFF).toFloat()

                val denominator = g + r - b
                // Sıfıra bölünmeyi önle (örn. tamamen siyah/beyaz düz alanlarda olabilir)
                if (Math.abs(denominator) > 1e-3f) {
                    val vari = (g - r) / denominator
                    sumVari += vari.coerceIn(-1f, 1f)
                    sampleCount++
                }

                x += stepX
            }
            y += stepY
        }

        return if (sampleCount > 0) (sumVari / sampleCount).toFloat() else 0f
    }

    /**
     * Görüntüyü bir ızgaraya bölüp her hücre için ayrı VARI hesaplar.
     * Bu, tek bir global ortalama yerine, fotoğraf içindeki yerel bitki örtüsü
     * KONTRASTINI (örn. fotoğrafın bir köşesi çıplak, diğeri yeşil) tespit etmek
     * için kullanılır — "crop mark" tespitinin temel mantığı budur.
     */
    fun estimateGridVariance(bitmap: Bitmap, gridCells: Int = 4): Float {
        val width = bitmap.width
        val height = bitmap.height
        if (width == 0 || height == 0 || gridCells < 2) return 0f

        val cellWidth = width / gridCells
        val cellHeight = height / gridCells
        if (cellWidth == 0 || cellHeight == 0) return 0f

        val cellVariValues = ArrayList<Float>(gridCells * gridCells)

        for (row in 0 until gridCells) {
            for (col in 0 until gridCells) {
                val startX = col * cellWidth
                val startY = row * cellHeight
                val endX = (startX + cellWidth).coerceAtMost(width)
                val endY = (startY + cellHeight).coerceAtMost(height)

                val cellBitmap = Bitmap.createBitmap(
                    bitmap, startX, startY, endX - startX, endY - startY
                )
                cellVariValues.add(estimate(cellBitmap, sampleGridSize = 8))
                cellBitmap.recycle()
            }
        }

        if (cellVariValues.isEmpty()) return 0f
        val mean = cellVariValues.average().toFloat()
        val variance = cellVariValues.sumOf { (it - mean).toDouble().let { d -> d * d } } / cellVariValues.size
        return Math.sqrt(variance).toFloat()
    }
}
