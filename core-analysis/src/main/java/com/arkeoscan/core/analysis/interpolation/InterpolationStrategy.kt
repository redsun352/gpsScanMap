package com.arkeoscan.core.analysis.interpolation

import com.arkeoscan.core.common.model.GeoPoint

/**
 * Mekansal interpolasyon stratejisi için ortak arayüz.
 * Şu an IDW (Inverse Distance Weighting) uygulanmıştır. İleride Kriging
 * (örn. Ordinary Kriging, variogram tabanlı) bu arayüzü implemente eden
 * yeni bir sınıf olarak eklenebilir; çağıran kod (GridGenerator) değişmez.
 */
interface InterpolationStrategy {
    /**
     * Bilinen ölçüm noktalarından (knownPoints + knownValues, aynı index sırasıyla
     * eşleşir), hedef noktadaki (targetLocal) değeri tahmin eder.
     */
    fun interpolate(
        knownLocalCoords: List<Pair<Double, Double>>,
        knownValues: List<Float>,
        targetLocal: Pair<Double, Double>
    ): Float

    val name: String
}

/**
 * Ölçüm noktasını yerel düzlemsel koordinata ve değerine eşleyen taşıyıcı.
 */
data class SpatialSample(
    val geoPoint: GeoPoint,
    val value: Float
)
