package com.arkeoscan.core.gps.polygon

import com.arkeoscan.core.common.model.GeoPoint
import com.arkeoscan.core.common.model.Polygon
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kullanıcı "Durdur" dediğinde, yürüdüğü temizlenmiş rota noktalarından
 * otomatik kapalı Polygon üretir. İlk nokta ile son nokta birleştirilir.
 *
 * Bu sınıf yalnızca rotanın kendisini (dış sınır olarak) kapatır; rota zaten
 * kabaca dışbükey/basit bir döngü çiziyorsa (saha taraması senaryosunda olağan
 * davranış budur) doğrudan kullanılabilir. Kendi kendini kesen karmaşık
 * rotalar için convex hull'a düşülür (güvenli fallback).
 */
@Singleton
class PolygonBuilder @Inject constructor() {

    fun buildFromRoute(routePoints: List<GeoPoint>): Polygon? {
        if (routePoints.size < 3) return null

        val closedRoute = closeLoop(routePoints)

        return if (isSimplePolygon(closedRoute)) {
            Polygon(closedRoute)
        } else {
            // Rota kendini kesiyorsa (örn. GPS gürültüsü nedeniyle), dışbükey gövdeye
            // (convex hull) düş — bu her zaman basit (kendini kesmeyen) bir polygon üretir.
            Polygon(convexHull(routePoints))
        }
    }

    /**
     * İlk nokta ile son noktayı birleştirerek döngüyü kapatır.
     * Polygon data class'ı zaten son->ilk kenarını örtük olarak ekler (contains/area
     * hesaplarında vertices[(i+1)%n] kullanılır), bu yüzden burada sadece kopya döneriz.
     */
    private fun closeLoop(points: List<GeoPoint>): List<GeoPoint> = points.toList()

    /**
     * Basit (self-intersecting olmayan) polygon kontrolü: ardışık olmayan kenarların
     * kesişip kesişmediğini O(n²) brute-force ile test eder. Walk Scan rotaları
     * tipik olarak yüzlerce nokta içerdiğinden bu kabul edilebilir maliyettir
     * (tek seferlik, STOP anında çalışır).
     */
    private fun isSimplePolygon(points: List<GeoPoint>): Boolean {
        val n = points.size
        if (n < 4) return true

        val local = points.map { p -> Polygon.projectToLocal(p, points.first()) }

        for (i in 0 until n) {
            val a1 = local[i]
            val a2 = local[(i + 1) % n]
            for (j in i + 1 until n) {
                // Komşu kenarları (ortak köşe paylaşanları) atla
                if (j == i || (j + 1) % n == i || j == (i + 1) % n) continue
                val b1 = local[j]
                val b2 = local[(j + 1) % n]
                if (segmentsIntersect(a1, a2, b1, b2)) {
                    return false
                }
            }
        }
        return true
    }

    private fun segmentsIntersect(
        p1: Pair<Double, Double>,
        p2: Pair<Double, Double>,
        p3: Pair<Double, Double>,
        p4: Pair<Double, Double>
    ): Boolean {
        fun cross(o: Pair<Double, Double>, a: Pair<Double, Double>, b: Pair<Double, Double>): Double =
            (a.first - o.first) * (b.second - o.second) - (a.second - o.second) * (b.first - o.first)

        val d1 = cross(p3, p4, p1)
        val d2 = cross(p3, p4, p2)
        val d3 = cross(p1, p2, p3)
        val d4 = cross(p1, p2, p4)

        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
            ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
    }

    /**
     * Andrew's Monotone Chain algoritması ile dışbükey gövde (convex hull) üretir.
     * Kendini kesen karmaşık rotalar için güvenli fallback.
     */
    private fun convexHull(points: List<GeoPoint>): List<GeoPoint> {
        val origin = points.first()
        val localPts = points.map { it to Polygon.projectToLocal(it, origin) }
            .distinctBy { it.second }
            .sortedWith(compareBy({ it.second.first }, { it.second.second }))

        if (localPts.size < 3) return points

        fun cross(o: Pair<Double, Double>, a: Pair<Double, Double>, b: Pair<Double, Double>): Double =
            (a.first - o.first) * (b.second - o.second) - (a.second - o.second) * (b.first - o.first)

        val lower = mutableListOf<Pair<GeoPoint, Pair<Double, Double>>>()
        for (p in localPts) {
            while (lower.size >= 2 && cross(lower[lower.size - 2].second, lower[lower.size - 1].second, p.second) <= 0) {
                lower.removeAt(lower.size - 1)
            }
            lower.add(p)
        }

        val upper = mutableListOf<Pair<GeoPoint, Pair<Double, Double>>>()
        for (p in localPts.reversed()) {
            while (upper.size >= 2 && cross(upper[upper.size - 2].second, upper[upper.size - 1].second, p.second) <= 0) {
                upper.removeAt(upper.size - 1)
            }
            upper.add(p)
        }

        lower.removeAt(lower.size - 1)
        upper.removeAt(upper.size - 1)

        return (lower + upper).map { it.first }
    }
}
