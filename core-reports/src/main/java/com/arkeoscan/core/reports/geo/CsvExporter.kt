package com.arkeoscan.core.reports.geo

import com.arkeoscan.core.common.model.AnomalyResult
import com.arkeoscan.core.common.model.InterpolatedGrid
import com.arkeoscan.core.common.model.ScanPoint
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor() {

    fun exportScanPoints(points: List<ScanPoint>, outputFile: File) {
        outputFile.bufferedWriter().use { writer ->
            writer.appendLine("sequence_index,latitude,longitude,altitude,gps_accuracy_m,magnetic_total_ut,magnetic_x,magnetic_y,magnetic_z,heading_deg,timestamp_millis")
            for (p in points) {
                writer.appendLine(
                    listOf(
                        p.sequenceIndex,
                        p.latitude,
                        p.longitude,
                        p.altitude ?: "",
                        p.gpsAccuracyMeters,
                        p.magneticTotalMicroTesla,
                        p.magneticX,
                        p.magneticY,
                        p.magneticZ,
                        p.headingDegrees ?: "",
                        p.timestampMillis
                    ).joinToString(",")
                )
            }
        }
    }

    fun exportGrid(grid: InterpolatedGrid, outputFile: File) {
        outputFile.bufferedWriter().use { writer ->
            writer.appendLine("row,col,latitude,longitude,local_x,local_y,value,inside_polygon")
            for (cell in grid.cells) {
                writer.appendLine(
                    listOf(
                        cell.row,
                        cell.col,
                        cell.latitude,
                        cell.longitude,
                        cell.localX,
                        cell.localY,
                        cell.value,
                        cell.isInsidePolygon
                    ).joinToString(",")
                )
            }
        }
    }

    fun exportAnomalies(anomalies: List<AnomalyResult>, outputFile: File) {
        outputFile.bufferedWriter().use { writer ->
            writer.appendLine("center_lat,center_lon,deviation_sigma,area_m2,shape,continuity,neighborhood,confidence,label")
            for (a in anomalies) {
                writer.appendLine(
                    listOf(
                        a.centerLatitude,
                        a.centerLongitude,
                        a.deviationSigma,
                        a.areaSquareMeters,
                        a.shape.name,
                        a.continuityScore,
                        a.neighborhoodScore,
                        a.confidenceScore,
                        "\"${a.label}\""
                    ).joinToString(",")
                )
            }
        }
    }
}
