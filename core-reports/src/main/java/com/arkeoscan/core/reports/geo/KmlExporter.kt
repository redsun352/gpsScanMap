package com.arkeoscan.core.reports.geo

import com.arkeoscan.core.common.model.AnomalyResult
import com.arkeoscan.core.common.model.Polygon
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KmlExporter @Inject constructor() {

    fun export(polygon: Polygon, anomalies: List<AnomalyResult>, outputFile: File, sessionName: String) {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<kml xmlns="http://www.opengis.net/kml/2.2">""")
        sb.appendLine("<Document>")
        sb.appendLine("<name>${escapeXml(sessionName)}</name>")

        sb.appendLine(
            """
            <Style id="polygonStyle">
              <LineStyle><color>ff00d7ff</color><width>2</width></LineStyle>
              <PolyStyle><color>3300d7ff</color></PolyStyle>
            </Style>
            <Style id="anomalyStyle">
              <IconStyle><color>ff0033ff</color><scale>1.1</scale></IconStyle>
            </Style>
            """.trimIndent()
        )

        // Polygon
        sb.appendLine("<Placemark>")
        sb.appendLine("<name>Tarama Alanı</name>")
        sb.appendLine("<styleUrl>#polygonStyle</styleUrl>")
        sb.appendLine("<Polygon><outerBoundaryIs><LinearRing><coordinates>")
        for (v in polygon.vertices) {
            sb.append("${v.longitude},${v.latitude},0 ")
        }
        val first = polygon.vertices.first()
        sb.append("${first.longitude},${first.latitude},0")
        sb.appendLine()
        sb.appendLine("</coordinates></LinearRing></outerBoundaryIs></Polygon>")
        sb.appendLine("</Placemark>")

        // Anomaliler
        for ((index, anomaly) in anomalies.withIndex()) {
            sb.appendLine("<Placemark>")
            sb.appendLine("<name>${escapeXml(anomaly.label)} #${index + 1}</name>")
            sb.appendLine("<styleUrl>#anomalyStyle</styleUrl>")
            sb.appendLine(
                "<description>${
                    escapeXml(
                        "Sapma: ${"%.2f".format(anomaly.deviationSigma)}σ, " +
                            "Alan: ${"%.1f".format(anomaly.areaSquareMeters)} m², " +
                            "Şekil: ${anomaly.shape.name}, " +
                            "Güven: ${"%.0f".format(anomaly.confidenceScore * 100)}%. " +
                            AnomalyResult.DISCLAIMER
                    )
                }</description>"
            )
            sb.appendLine("<Point><coordinates>${anomaly.centerLongitude},${anomaly.centerLatitude},0</coordinates></Point>")
            sb.appendLine("</Placemark>")
        }

        sb.appendLine("</Document>")
        sb.appendLine("</kml>")

        outputFile.writeText(sb.toString())
    }

    private fun escapeXml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
