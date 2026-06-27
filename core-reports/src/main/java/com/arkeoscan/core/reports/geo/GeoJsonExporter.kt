package com.arkeoscan.core.reports.geo

import com.arkeoscan.core.common.model.AnomalyResult
import com.arkeoscan.core.common.model.InterpolatedGrid
import com.arkeoscan.core.common.model.Polygon
import com.google.gson.GsonBuilder
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoJsonExporter @Inject constructor() {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun exportPolygon(polygon: Polygon, outputFile: File) {
        val coordinates = polygon.vertices.map { listOf(it.longitude, it.latitude) } +
            listOf(listOf(polygon.vertices.first().longitude, polygon.vertices.first().latitude))

        val geoJson = mapOf(
            "type" to "FeatureCollection",
            "features" to listOf(
                mapOf(
                    "type" to "Feature",
                    "properties" to mapOf(
                        "areaSquareMeters" to polygon.areaSquareMeters(),
                        "perimeterMeters" to polygon.perimeterMeters()
                    ),
                    "geometry" to mapOf(
                        "type" to "Polygon",
                        "coordinates" to listOf(coordinates)
                    )
                )
            )
        )
        outputFile.writeText(gson.toJson(geoJson))
    }

    fun exportAnomalies(anomalies: List<AnomalyResult>, outputFile: File) {
        val features = anomalies.map { a ->
            mapOf(
                "type" to "Feature",
                "properties" to mapOf(
                    "label" to a.label,
                    "deviationSigma" to a.deviationSigma,
                    "areaSquareMeters" to a.areaSquareMeters,
                    "shape" to a.shape.name,
                    "continuityScore" to a.continuityScore,
                    "neighborhoodScore" to a.neighborhoodScore,
                    "confidenceScore" to a.confidenceScore,
                    "disclaimer" to AnomalyResult.DISCLAIMER
                ),
                "geometry" to mapOf(
                    "type" to "Point",
                    "coordinates" to listOf(a.centerLongitude, a.centerLatitude)
                )
            )
        }
        val geoJson = mapOf("type" to "FeatureCollection", "features" to features)
        outputFile.writeText(gson.toJson(geoJson))
    }

    /**
     * Grid'i, polygon içindeki hücreler için renk/yoğunluk gösteren nokta
     * koleksiyonu olarak dışa aktarır (heatmap'in GIS yazılımlarında —QGIS vb.—
     * yeniden açılabilmesi için).
     */
    fun exportGridAsPoints(grid: InterpolatedGrid, outputFile: File) {
        val features = grid.cells.filter { it.isInsidePolygon }.map { cell ->
            mapOf(
                "type" to "Feature",
                "properties" to mapOf(
                    "value" to cell.value,
                    "row" to cell.row,
                    "col" to cell.col
                ),
                "geometry" to mapOf(
                    "type" to "Point",
                    "coordinates" to listOf(cell.longitude, cell.latitude)
                )
            )
        }
        val geoJson = mapOf("type" to "FeatureCollection", "features" to features)
        outputFile.writeText(gson.toJson(geoJson))
    }
}
