package com.arkeoscan.core.reports.di

import android.content.Context
import android.graphics.Bitmap
import com.arkeoscan.core.common.model.AnomalyResult
import com.arkeoscan.core.common.model.InterpolatedGrid
import com.arkeoscan.core.common.model.Polygon
import com.arkeoscan.core.common.model.ScanPoint
import com.arkeoscan.core.common.model.ScanSession
import com.arkeoscan.core.reports.geo.CsvExporter
import com.arkeoscan.core.reports.geo.GeoJsonExporter
import com.arkeoscan.core.reports.geo.KmlExporter
import com.arkeoscan.core.reports.pdf.PdfReportGenerator
import com.arkeoscan.core.reports.pdf.PngExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Rapor: PDF, PNG, CSV, GeoJSON, KML oluştur" gereksinimi için tek giriş noktası.
 * Tüm dosyalar context.getExternalFilesDir(null)/reports/session_<id>/ altına yazılır.
 */
@Singleton
class ReportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val csvExporter: CsvExporter,
    private val geoJsonExporter: GeoJsonExporter,
    private val kmlExporter: KmlExporter,
    private val pdfReportGenerator: PdfReportGenerator,
    private val pngExporter: PngExporter
) {
    fun reportsDir(sessionId: Long): File {
        val dir = File(context.getExternalFilesDir(null), "reports/session_$sessionId")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun exportAllFormats(
        session: ScanSession,
        scanPoints: List<ScanPoint>,
        grid: InterpolatedGrid?,
        anomalies: List<AnomalyResult>,
        screenshot: Bitmap?
    ): ExportedFiles {
        val dir = reportsDir(session.id)

        val csvPointsFile = File(dir, "scan_points.csv").also { csvExporter.exportScanPoints(scanPoints, it) }
        val csvAnomaliesFile = File(dir, "anomalies.csv").also { csvExporter.exportAnomalies(anomalies, it) }

        var geoJsonPolygonFile: File? = null
        var geoJsonAnomaliesFile: File? = null
        var kmlFile: File? = null

        session.polygon?.let { polygon: Polygon ->
            geoJsonPolygonFile = File(dir, "polygon.geojson").also { geoJsonExporter.exportPolygon(polygon, it) }
            geoJsonAnomaliesFile = File(dir, "anomalies.geojson").also { geoJsonExporter.exportAnomalies(anomalies, it) }
            kmlFile = File(dir, "${session.name}.kml").also {
                kmlExporter.export(polygon, anomalies, it, session.name)
            }
        }

        val pdfFile = File(dir, "report.pdf").also {
            pdfReportGenerator.generate(session, anomalies, it, screenshot)
        }

        var pngFile: File? = null
        screenshot?.let { bmp ->
            pngFile = File(dir, "screenshot.png").also { pngExporter.export(bmp, it) }
        }

        return ExportedFiles(
            csvScanPoints = csvPointsFile,
            csvAnomalies = csvAnomaliesFile,
            geoJsonPolygon = geoJsonPolygonFile,
            geoJsonAnomalies = geoJsonAnomaliesFile,
            kml = kmlFile,
            pdf = pdfFile,
            png = pngFile
        )
    }

    data class ExportedFiles(
        val csvScanPoints: File,
        val csvAnomalies: File,
        val geoJsonPolygon: File?,
        val geoJsonAnomalies: File?,
        val kml: File?,
        val pdf: File,
        val png: File?
    )
}
