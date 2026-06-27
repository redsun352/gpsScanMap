package com.arkeoscan.phone.ui.screens.arealysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkeoscan.core.analysis.anomaly.AnomalyEngine
import com.arkeoscan.core.analysis.grid.GridGenerator
import com.arkeoscan.core.analysis.interpolation.IdwInterpolation
import com.arkeoscan.core.common.model.AnomalyResult
import com.arkeoscan.core.common.model.GeoPoint
import com.arkeoscan.core.common.model.GridResolution
import com.arkeoscan.core.common.model.InterpolatedGrid
import com.arkeoscan.core.common.model.Polygon
import com.arkeoscan.core.database.dao.AnomalyResultDao
import com.arkeoscan.core.database.dao.ScanPointDao
import com.arkeoscan.core.database.dao.ScanSessionDao
import com.arkeoscan.core.database.entity.AnomalyResultEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AreaAnalysisUiState(
    val isLoading: Boolean = false,
    val sessionId: Long? = null,
    val polygon: Polygon? = null,
    val areaSquareMeters: Double? = null,
    val perimeterMeters: Double? = null,
    val grid: InterpolatedGrid? = null,
    val anomalies: List<AnomalyResult> = emptyList(),
    val gridResolution: GridResolution = GridResolution.ONE_METER,
    val errorMessage: String? = null
)

/**
 * Tarama tamamlandıktan sonra (STOP), kullanıcı bu ekranda en son oturumun
 * Polygon + alan bilgisini görür ve "Analiz Et" ile IDW grid üretimi ve
 * Anomaly Engine'i tetikleyebilir. Sonuçlar Heatmap/Contour/3D Surface
 * ekranlarına aktarılmak üzere bu ViewModel'de (ve DB'de) saklanır.
 */
@HiltViewModel
class AreaAnalysisViewModel @Inject constructor(
    private val scanSessionDao: ScanSessionDao,
    private val scanPointDao: ScanPointDao,
    private val anomalyResultDao: AnomalyResultDao,
    private val gridGenerator: GridGenerator,
    private val idwInterpolation: IdwInterpolation,
    private val anomalyEngine: AnomalyEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(AreaAnalysisUiState())
    val uiState: StateFlow<AreaAnalysisUiState> = _uiState.asStateFlow()

    init {
        loadLatestSession()
    }

    private fun loadLatestSession() {
        viewModelScope.launch {
            val session = scanSessionDao.getActiveSession() ?: run {
                // Aktif oturum yoksa, en son tamamlanmış (STOPPED) oturumu da bulmaya çalışabiliriz;
                // basitlik için burada şimdilik sadece aktif/duraklatılmış oturum aranıyor,
                // observeAll() Flow'undan UI katmanı liste de gösterebilir.
                null
            }
            session?.polygon()?.let { polygon ->
                _uiState.update {
                    it.copy(
                        sessionId = session.id,
                        polygon = polygon,
                        areaSquareMeters = session.areaSquareMeters,
                        perimeterMeters = session.perimeterMeters
                    )
                }
            }
        }
    }

    fun setGridResolution(resolution: GridResolution) {
        _uiState.update { it.copy(gridResolution = resolution) }
    }

    fun runAnalysis() {
        val sessionId = _uiState.value.sessionId ?: return
        val polygon = _uiState.value.polygon ?: return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val scanPoints = scanPointDao.getPointsForSession(sessionId)
                if (scanPoints.isEmpty()) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Bu oturum için kayıtlı ölçüm noktası yok.")
                    }
                    return@launch
                }

                val samples = scanPoints.map { p ->
                    GeoPoint(latitude = p.latitude, longitude = p.longitude) to p.magneticTotalMicroTesla
                }

                val resolution = _uiState.value.gridResolution.meters
                val grid = gridGenerator.generate(polygon, samples, resolution, idwInterpolation)
                val anomalies = anomalyEngine.detect(sessionId, grid)

                anomalyResultDao.deleteAllForSession(sessionId)
                anomalyResultDao.insertAll(
                    anomalies.map { a ->
                        AnomalyResultEntity(
                            sessionId = sessionId,
                            centerLatitude = a.centerLatitude,
                            centerLongitude = a.centerLongitude,
                            deviationSigma = a.deviationSigma,
                            areaSquareMeters = a.areaSquareMeters,
                            shape = a.shape.name,
                            continuityScore = a.continuityScore,
                            neighborhoodScore = a.neighborhoodScore,
                            confidenceScore = a.confidenceScore,
                            label = a.label
                        )
                    }
                )

                _uiState.update {
                    it.copy(isLoading = false, grid = grid, anomalies = anomalies)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Analiz hatası: ${e.message}")
                }
            }
        }
    }

    private fun com.arkeoscan.core.database.entity.ScanSessionEntity.polygon(): Polygon? {
        val wkt = this.polygonWkt ?: return null
        val points = wkt.split(";").mapNotNull { pair ->
            val parts = pair.split(",")
            if (parts.size != 2) return@mapNotNull null
            val lat = parts[0].toDoubleOrNull() ?: return@mapNotNull null
            val lon = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            GeoPoint(latitude = lat, longitude = lon)
        }
        return if (points.size >= 3) Polygon(points) else null
    }
}
