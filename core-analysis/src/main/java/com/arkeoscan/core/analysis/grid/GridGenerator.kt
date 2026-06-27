package com.arkeoscan.core.analysis.grid

import com.arkeoscan.core.analysis.interpolation.InterpolationStrategy
import com.arkeoscan.core.common.model.GridCell
import com.arkeoscan.core.common.model.InterpolatedGrid
import com.arkeoscan.core.common.model.Polygon
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seçilen grid çözünürlüğüne (0.5/1/2 m) göre Polygon'un bounding box'ını
 * hücrelere böler, her hücre merkezi için interpolasyon stratejisini çalıştırır
 * ve Polygon dışındaki hücreleri NoData (isInsidePolygon=false) olarak işaretler.
 *
 * "Bütün analizler yalnızca Polygon içinde çalışsın" ve "Polygon dışındaki tüm
 * gridler silinsin" gereksinimleri burada uygulanır: dışarıdaki hücreler value
 * hesabına dahil edilir (render katmanı kolaylığı için) ama isInsidePolygon=false
 * bayrağıyla işaretlenir; tüm downstream tüketiciler (Heatmap, Contour, 3D Surface,
 * Anomaly Engine) bu bayrağa göre filtrelemelidir.
 */
@Singleton
class GridGenerator @Inject constructor() {

    fun generate(
        polygon: Polygon,
        samples: List<Pair<com.arkeoscan.core.common.model.GeoPoint, Float>>,
        resolutionMeters: Double,
        strategy: InterpolationStrategy
    ): InterpolatedGrid {
        val bbox = polygon.boundingBoxLocalMeters()
        val origin = polygon.vertices.first()

        val cols = Math.max(1, Math.ceil(bbox.width / resolutionMeters).toInt())
        val rows = Math.max(1, Math.ceil(bbox.height / resolutionMeters).toInt())

        val knownLocalCoords = samples.map { (geoPoint, _) ->
            Polygon.projectToLocal(geoPoint, origin)
        }
        val knownValues = samples.map { it.second }

        val cells = ArrayList<GridCell>(rows * cols)
        var minVal = Float.MAX_VALUE
        var maxVal = Float.MIN_VALUE
        var sum = 0.0

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val localX = bbox.minX + (col + 0.5) * resolutionMeters
                val localY = bbox.minY + (row + 0.5) * resolutionMeters

                val value = if (knownLocalCoords.isNotEmpty()) {
                    strategy.interpolate(knownLocalCoords, knownValues, Pair(localX, localY))
                } else {
                    0f
                }

                val geoPoint = localToGeo(localX, localY, origin)
                val inside = polygon.contains(geoPoint)

                if (inside) {
                    if (value < minVal) minVal = value
                    if (value > maxVal) maxVal = value
                    sum += value
                }

                cells.add(
                    GridCell(
                        row = row,
                        col = col,
                        localX = localX,
                        localY = localY,
                        latitude = geoPoint.latitude,
                        longitude = geoPoint.longitude,
                        value = value,
                        isInsidePolygon = inside
                    )
                )
            }
        }

        val insideCount = cells.count { it.isInsidePolygon }
        val mean = if (insideCount > 0) (sum / insideCount).toFloat() else 0f

        if (minVal == Float.MAX_VALUE) minVal = 0f
        if (maxVal == Float.MIN_VALUE) maxVal = 0f

        return InterpolatedGrid(
            cells = cells,
            rows = rows,
            cols = cols,
            resolutionMeters = resolutionMeters,
            minValue = minVal,
            maxValue = maxVal,
            meanValue = mean
        )
    }

    /**
     * Yerel metre koordinatından (equirectangular yaklaşıklığının tersi) tekrar
     * lat/lon'a döner. Polygon.projectToLocal'ın matematiksel tersidir.
     */
    private fun localToGeo(
        localX: Double,
        localY: Double,
        origin: com.arkeoscan.core.common.model.GeoPoint
    ): com.arkeoscan.core.common.model.GeoPoint {
        val earthRadius = 6371000.0
        val meanLat = Math.toRadians(origin.latitude) // küçük alanlarda origin enlemiyle yaklaşıklık yeterli
        val dLat = localY / earthRadius
        val dLon = localX / (earthRadius * Math.cos(meanLat))

        val lat = origin.latitude + Math.toDegrees(dLat)
        val lon = origin.longitude + Math.toDegrees(dLon)
        return com.arkeoscan.core.common.model.GeoPoint(latitude = lat, longitude = lon)
    }
}
