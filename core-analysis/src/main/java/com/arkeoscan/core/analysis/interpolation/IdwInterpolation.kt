package com.arkeoscan.core.analysis.interpolation

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Inverse Distance Weighting (IDW) interpolasyonu.
 *
 * value(target) = Σ(w_i * v_i) / Σ(w_i),  w_i = 1 / d_i^power
 *
 * @property power Mesafe ağırlık üssü. Tipik değer 2.0; daha yüksek değer
 *   yakın noktaların etkisini güçlendirir (daha "keskin" anomali sınırları),
 *   daha düşük değer daha yumuşak/genel bir yüzey üretir.
 * @property searchRadiusMeters null ise tüm noktalar kullanılır; bir değer
 *   verilirse sadece bu yarıçap içindeki noktalar dahil edilir (performans ve
 *   yerel duyarlılık için, büyük taramalarda önerilir).
 * @property maxNeighbors Arama yarıçapı içinde bulunsa da en yakın N nokta ile sınırlar
 *   (performans optimizasyonu, büyük nokta bulutlarında O(n) maliyeti sınırlar).
 */
@Singleton
class IdwInterpolation @Inject constructor() : InterpolationStrategy {

    override val name: String = "IDW"

    var power: Double = 2.0
    var searchRadiusMeters: Double? = null
    var maxNeighbors: Int = 12

    override fun interpolate(
        knownLocalCoords: List<Pair<Double, Double>>,
        knownValues: List<Float>,
        targetLocal: Pair<Double, Double>
    ): Float {
        require(knownLocalCoords.size == knownValues.size) {
            "Koordinat ve değer listeleri aynı uzunlukta olmalı"
        }
        if (knownLocalCoords.isEmpty()) return 0f

        // Mesafeleri hesapla, çok küçük mesafede (örnek noktanın üzerinde) direkt değeri döndür.
        val distances = knownLocalCoords.mapIndexed { idx, coord ->
            val dx = coord.first - targetLocal.first
            val dy = coord.second - targetLocal.second
            val d = sqrt(dx * dx + dy * dy)
            idx to d
        }

        val exactMatch = distances.firstOrNull { it.second < 1e-6 }
        if (exactMatch != null) {
            return knownValues[exactMatch.first]
        }

        var candidates = distances
        searchRadiusMeters?.let { radius ->
            candidates = candidates.filter { it.second <= radius }
        }

        if (candidates.isEmpty()) {
            // Yarıçap içinde nokta yoksa, en yakın komşuya geri düş (NoData üretmemek için).
            candidates = distances
        }

        candidates = candidates.sortedBy { it.second }.take(maxNeighbors)

        var weightedSum = 0.0
        var weightTotal = 0.0
        for ((idx, d) in candidates) {
            val w = 1.0 / d.pow(power)
            weightedSum += w * knownValues[idx]
            weightTotal += w
        }

        return if (weightTotal > 0.0) (weightedSum / weightTotal).toFloat() else 0f
    }
}
