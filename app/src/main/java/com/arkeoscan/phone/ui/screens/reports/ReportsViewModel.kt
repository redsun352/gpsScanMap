package com.arkeoscan.phone.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkeoscan.core.common.model.AnomalyResult
import com.arkeoscan.core.common.model.AnomalyShape
import com.arkeoscan.core.common.model.GeoPoint
import com.arkeoscan.core.common.model.Polygon
import com.arkeoscan.core.common.model.ScanPoint
import com.arkeoscan.core.common.model.ScanSession
import com.arkeoscan.core.common.model.ScanSessionStatus
import com.arkeoscan.core.database.dao.AnomalyResultDao
import com.arkeoscan.core.database.dao.ScanPointDao
import com.arkeoscan.core.database.dao.ScanSessionDao
import com.arkeoscan.core.reports.di.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ReportsUiState(
    val isExporting: Boolean = false,
    val exportedFiles: ReportRepository.ExportedFiles? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val scanSessionDao: ScanSessionDao,
    private val scanPointDao: ScanPointDao,
    private val anomalyResultDao: AnomalyResultDao,
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    fun exportLatestSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }
            try {
                val sessionEntity = scanSessionDao.getActiveSession()
                    ?: run {
                        _uiState.update {
                            it.copy(isExporting = false, errorMessage = "Dışa aktarılacak bir oturum bulunamadı.")
                        }
                        return@launch
                    }

                val scanPointEntities = scanPointDao.getPointsForSession(sessionEntity.id)
                val scanPoints = scanPointEntities.map { p ->
                    ScanPoint(
                        id = p.id,
                        sessionId = p.sessionId,
                        sequenceIndex = p.sequenceIndex,
                        latitude = p.latitude,
                        longitude = p.longitude,
                        altitude = p.altitude,
                        gpsAccuracyMeters = p.gpsAccuracyMeters,
                        magneticTotalMicroTesla = p.magneticTotalMicroTesla,
                        magneticX = p.magneticX,
                        magneticY = p.magneticY,
                        magneticZ = p.magneticZ,
                        headingDegrees = p.headingDegrees,
                        timestampMillis = p.timestampMillis
                    )
                }

                val polygon = sessionEntity.polygonWkt?.let { wkt ->
                    val points = wkt.split(";").mapNotNull { pair ->
                        val parts = pair.split(",")
                        if (parts.size != 2) return@mapNotNull null
                        val lat = parts[0].toDoubleOrNull() ?: return@mapNotNull null
                        val lon = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                        GeoPoint(latitude = lat, longitude = lon)
                    }
                    if (points.size >= 3) Polygon(points) else null
                }

                val session = ScanSession(
                    id = sessionEntity.id,
                    name = sessionEntity.name,
                    startedAtMillis = sessionEntity.startedAtMillis,
                    endedAtMillis = sessionEntity.endedAtMillis,
                    polygon = polygon,
                    areaSquareMeters = sessionEntity.areaSquareMeters,
                    perimeterMeters = sessionEntity.perimeterMeters,
                    gridResolutionMeters = sessionEntity.gridResolutionMeters,
                    status = runCatching { ScanSessionStatus.valueOf(sessionEntity.status) }
                        .getOrDefault(ScanSessionStatus.STOPPED)
                )

                val anomalies = anomalyResultDao.observeForSession(sessionEntity.id)

                anomalies.first().let { entities ->
                    val mapped = entities.map { e ->
                        AnomalyResult(
                            id = e.id,
                            sessionId = e.sessionId,
                            centerLatitude = e.centerLatitude,
                            centerLongitude = e.centerLongitude,
                            deviationSigma = e.deviationSigma,
                            areaSquareMeters = e.areaSquareMeters,
                            shape = runCatching { AnomalyShape.valueOf(e.shape) }.getOrDefault(AnomalyShape.UNKNOWN),
                            continuityScore = e.continuityScore,
                            neighborhoodScore = e.neighborhoodScore,
                            confidenceScore = e.confidenceScore,
                            label = e.label
                        )
                    }

                    val exported = reportRepository.exportAllFormats(
                        session = session,
                        scanPoints = scanPoints,
                        grid = null,
                        anomalies = mapped,
                        screenshot = null
                    )

                    _uiState.update { it.copy(isExporting = false, exportedFiles = exported) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isExporting = false, errorMessage = "Dışa aktarma hatası: ${e.message}")
                }
            }
        }
    }
}
