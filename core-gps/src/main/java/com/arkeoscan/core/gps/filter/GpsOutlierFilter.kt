package com.arkeoscan.core.gps.filter

import com.arkeoscan.core.common.model.GeoPoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GPS Temizleme:
 * - GPS sıçramalarını sil (fiziksel olarak mümkün olmayan hız sıçramaları)
 * - Hatalı noktaları kaldır (düşük doğruluk / accuracy eşiği üstü)
 * - Outlier filtresi uygula (komşu noktalara göre anormal sapma)
 * - Yakın noktaları birleştir (bekleme sırasında oluşan gereksiz kayıtları azalt)
 */
@Singleton
class GpsOutlierFilter @Inject constructor() {

    /**
     * @param maxAccuracyMeters Bu değerden daha kötü (büyük) doğruluğa sahip noktalar atılır.
     * @param maxPlausibleSpeedMps Yürüyen bir insan için fizksel olarak anlamlı üst hız sınırı.
     *   Bunun üzerindeki ardışık nokta sıçramaları GPS hatası kabul edilir (varsayılan 8 m/s ~ koşu/yanlış sıçrama sınırı).
     * @param mergeRadiusMeters Bu yarıçaptan daha yakın ardışık noktalar tek noktaya indirilir
     *   (kullanıcı durup beklediğinde oluşan gereksiz yoğun kayıtları azaltmak için).
     */
    fun clean(
        rawPoints: List<GeoPoint>,
        maxAccuracyMeters: Float = 20f,
        maxPlausibleSpeedMps: Float = 8f,
        mergeRadiusMeters: Double = 0.3
    ): List<GeoPoint> {
        if (rawPoints.isEmpty()) return emptyList()

        val accuracyFiltered = rawPoints.filter { it.accuracyMeters <= maxAccuracyMeters }
        if (accuracyFiltered.isEmpty()) return emptyList()

        val speedFiltered = removeSpeedSpikes(accuracyFiltered, maxPlausibleSpeedMps)
        val merged = mergeNearbyPoints(speedFiltered, mergeRadiusMeters)

        return merged
    }

    /**
     * Ardışık noktalar arasındaki zaman farkına göre gerçekleşmesi fiziksel olarak
     * mümkün olmayan hız sıçramalarını (GPS multipath/sıçrama hatası) tespit edip atar.
     */
    private fun removeSpeedSpikes(points: List<GeoPoint>, maxSpeedMps: Float): List<GeoPoint> {
        if (points.size < 2) return points

        val result = mutableListOf(points.first())
        for (i in 1 until points.size) {
            val prev = result.last()
            val candidate = points[i]
            val dtSeconds = (candidate.timestampMillis - prev.timestampMillis) / 1000.0
            if (dtSeconds <= 0) {
                // Aynı veya ters zaman damgası -> şüpheli, atla
                continue
            }
            val distance = prev.distanceMetersTo(candidate)
            val impliedSpeed = distance / dtSeconds

            if (impliedSpeed <= maxSpeedMps) {
                result.add(candidate)
            }
            // impliedSpeed çok yüksekse bu nokta bir sıçrama kabul edilir ve atılır;
            // bir sonraki nokta yine `prev` (son kabul edilen nokta) ile karşılaştırılır.
        }
        return result
    }

    /**
     * Ardışık noktalar arasındaki mesafe mergeRadiusMeters'tan küçükse,
     * kullanıcı duruyor/bekliyor kabul edilir ve sadece ilk nokta tutulur.
     */
    private fun mergeNearbyPoints(points: List<GeoPoint>, mergeRadiusMeters: Double): List<GeoPoint> {
        if (points.size < 2) return points

        val result = mutableListOf(points.first())
        for (i in 1 until points.size) {
            val last = result.last()
            val candidate = points[i]
            if (last.distanceMetersTo(candidate) >= mergeRadiusMeters) {
                result.add(candidate)
            }
        }
        return result
    }

    /**
     * Komşuluk bazlı istatistiksel outlier tespiti: bir noktanın önceki ve sonraki
     * noktalarla oluşturduğu üçgenin "sapma açısı" çok düşükse (neredeyse geri dönüş,
     * 150°+ keskin Z sıçraması) ve mesafe küçükse, bu nokta GPS gürültüsü kabul edilip elenir.
     * Walk Scan rotası genelde düzgün bir yürüyüş eğrisi izler; ani zikzaklar fiziksel olarak
     * anlamsızdır.
     */
    fun removeZigzagNoise(points: List<GeoPoint>, angleThresholdDegrees: Double = 150.0): List<GeoPoint> {
        if (points.size < 3) return points

        val result = mutableListOf(points.first())
        var i = 1
        while (i < points.size - 1) {
            val prev = result.last()
            val current = points[i]
            val next = points[i + 1]

            val bearingIn = prev.bearingTo(current)
            val bearingOut = current.bearingTo(next)
            var angleDiff = Math.abs(bearingOut - bearingIn)
            if (angleDiff > 180) angleDiff = 360 - angleDiff

            val isSharpZigzag = angleDiff >= angleThresholdDegrees
            val isShortHop = prev.distanceMetersTo(current) < 2.0

            if (isSharpZigzag && isShortHop) {
                // current'ı gürültü kabul et, atla
                i++
                continue
            }
            result.add(current)
            i++
        }
        result.add(points.last())
        return result
    }
}
