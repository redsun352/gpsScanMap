package com.arkeoscan.core.common.model

/**
 * Yürüme rotasından otomatik üretilen kapalı alan (polygon).
 * İlk nokta ile son nokta birleştirilerek kapatılır.
 *
 * Tüm analizler (heatmap, contour, 3D surface, anomali motoru) sadece
 * bu polygon'un içinde çalışır; dışındaki gridler maskelenir.
 */
data class Polygon(
    val vertices: List<GeoPoint>
) {
    init {
        require(vertices.size >= 3) { "Polygon en az 3 nokta içermeli" }
    }

    /**
     * Düzlemsel (yerel ENU) projeksiyon kullanarak alanı m² olarak hesaplar.
     * Shoelace (Gauss) formülü uygulanır. Küçük alanlar (saha taraması ölçeği,
     * tipik olarak < birkaç km²) için düzlemsel yaklaşım yeterli doğruluk sağlar.
     */
    fun areaSquareMeters(): Double {
        val local = toLocalMeters()
        var sum = 0.0
        val n = local.size
        for (i in 0 until n) {
            val (x1, y1) = local[i]
            val (x2, y2) = local[(i + 1) % n]
            sum += x1 * y2 - x2 * y1
        }
        return Math.abs(sum) / 2.0
    }

    /**
     * Çevre uzunluğunu metre olarak hesaplar (kapalı poligon kabul edilerek).
     */
    fun perimeterMeters(): Double {
        var total = 0.0
        val n = vertices.size
        for (i in 0 until n) {
            val a = vertices[i]
            val b = vertices[(i + 1) % n]
            total += a.distanceMetersTo(b)
        }
        return total
    }

    /**
     * Verilen GeoPoint'in polygon içinde olup olmadığını ray-casting algoritmasıyla test eder.
     * Yerel düzlemsel koordinatlara projekte edilerek hesaplanır.
     */
    fun contains(point: GeoPoint): Boolean {
        val local = toLocalMeters()
        val origin = vertices.first()
        val (px, py) = projectToLocal(point, origin)

        var inside = false
        var j = local.size - 1
        for (i in local.indices) {
            val (xi, yi) = local[i]
            val (xj, yj) = local[j]
            if ((yi > py) != (yj > py)) {
                val slopeX = xi + (py - yi) / (yj - yi) * (xj - xi)
                if (px < slopeX) inside = !inside
            }
            j = i
        }
        return inside
    }

    /**
     * Polygon'un bounding box'ını yerel metre koordinatlarında döner: (minX, minY, maxX, maxY)
     */
    fun boundingBoxLocalMeters(): BoundingBox {
        val local = toLocalMeters()
        val minX = local.minOf { it.first }
        val maxX = local.maxOf { it.first }
        val minY = local.minOf { it.second }
        val maxY = local.maxOf { it.second }
        return BoundingBox(minX, minY, maxX, maxY)
    }

    /**
     * Tüm köşeleri ilk noktayı orijin alan yerel düzlemsel (ENU benzeri) metre
     * koordinat sistemine çevirir. Equirectangular yaklaşıklığı kullanır;
     * saha taraması ölçeğinde (< birkaç km) hata ihmal edilebilir düzeydedir.
     */
    private fun toLocalMeters(): List<Pair<Double, Double>> {
        val origin = vertices.first()
        return vertices.map { projectToLocal(it, origin) }
    }

    companion object {
        private const val EARTH_RADIUS_M = 6371000.0

        fun projectToLocal(point: GeoPoint, origin: GeoPoint): Pair<Double, Double> {
            val dLat = Math.toRadians(point.latitude - origin.latitude)
            val dLon = Math.toRadians(point.longitude - origin.longitude)
            val meanLat = Math.toRadians((point.latitude + origin.latitude) / 2.0)
            val x = dLon * EARTH_RADIUS_M * Math.cos(meanLat)
            val y = dLat * EARTH_RADIUS_M
            return Pair(x, y)
        }
    }
}

data class BoundingBox(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double
) {
    val width: Double get() = maxX - minX
    val height: Double get() = maxY - minY
}
