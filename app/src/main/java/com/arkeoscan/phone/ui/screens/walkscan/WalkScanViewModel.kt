package com.arkeoscan.phone.ui.screens.walkscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkeoscan.core.common.model.GeoPoint
import com.arkeoscan.core.common.model.MagnetometerSample
import com.arkeoscan.core.common.model.Polygon
import com.arkeoscan.core.common.model.ScanPoint
import com.arkeoscan.core.common.model.ScanSession
import com.arkeoscan.core.common.model.ScanSessionStatus
import com.arkeoscan.core.database.dao.ScanPointDao
import com.arkeoscan.core.database.dao.ScanSessionDao
import com.arkeoscan.core.database.entity.ScanPointEntity
import com.arkeoscan.core.database.entity.ScanSessionEntity
import com.arkeoscan.core.gps.filter.GpsOutlierFilter
import com.arkeoscan.core.gps.polygon.PolygonBuilder
import com.arkeoscan.core.gps.tracker.LocationTracker
import com.arkeoscan.core.magnetometer.MagnetometerSensorManager
import com.arkeoscan.core.motion.HeadingProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WalkScanState { IDLE, RUNNING, PAUSED, STOPPED }

data class WalkScanUiState(
    val state: WalkScanState = WalkScanState.IDLE,
    val routePoints: List<GeoPoint> = emptyList(),
    val currentMagneticTotal: Float = 0f,
    val currentHeading: Float? = null,
    val recordedPointCount: Int = 0,
    val polygon: Polygon? = null,
    val areaSquareMeters: Double? = null,
    val perimeterMeters: Double? = null,
    val sessionId: Long? = null,
    val errorMessage: String? = null
)

/**
 * Walk Scan ekranının iş mantığı.
 * START: GPS+Mag+Gyro+Accel başlar, heading hesaplanır, her ~1 metrede kayıt oluşur.
 * STOP: Kayıt durur, GPS noktaları temizlenir+birleştirilir, Polygon oluşur, alan hesaplanır.
 */
@HiltViewModel
class WalkScanViewModel @Inject constructor(
    private val locationTracker: LocationTracker,
    private val magnetometerSensorManager: MagnetometerSensorManager,
    private val headingProvider: HeadingProvider,
    private val gpsOutlierFilter: GpsOutlierFilter,
    private val polygonBuilder: PolygonBuilder,
    private val scanSessionDao: ScanSessionDao,
    private val scanPointDao: ScanPointDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalkScanUiState())
    val uiState: StateFlow<WalkScanUiState> = _uiState.asStateFlow()

    private var locationJob: Job? = null
    private var magnetometerJob: Job? = null
    private var headingJob: Job? = null

    private var latestMagneticSample: MagnetometerSample? = null
    private var latestHeading: Float? = null
    private var sequenceCounter = 0
    private val rawRoutePoints = mutableListOf<GeoPoint>()

    fun start() {
        if (_uiState.value.state == WalkScanState.RUNNING) return

        viewModelScope.launch {
            val session = ScanSessionEntity(
                name = "Tarama ${System.currentTimeMillis()}",
                startedAtMillis = System.currentTimeMillis(),
                status = ScanSessionStatus.RUNNING.name
            )
            val sessionId = scanSessionDao.insert(session)
            _uiState.update { it.copy(sessionId = sessionId, state = WalkScanState.RUNNING) }

            startSensorCollection(sessionId)
        }
    }

    private fun startSensorCollection(sessionId: Long) {
        magnetometerJob = viewModelScope.launch {
            magnetometerSensorManager.sampleFlow().collect { sample ->
                latestMagneticSample = sample
                _uiState.update { it.copy(currentMagneticTotal = sample.totalMicroTesla) }
            }
        }

        headingJob = viewModelScope.launch {
            headingProvider.headingFlow().collect { heading ->
                latestHeading = heading
                _uiState.update { it.copy(currentHeading = heading) }
            }
        }

        locationJob = viewModelScope.launch {
            locationTracker.trackLocation(minIntervalMillis = 1000L, minDisplacementMeters = 1.0f)
                .collect { geoPoint ->
                    if (_uiState.value.state != WalkScanState.RUNNING) return@collect

                    rawRoutePoints.add(geoPoint)
                    _uiState.update { it.copy(routePoints = rawRoutePoints.toList()) }

                    val magSample = latestMagneticSample
                    val heading = latestHeading

                    val scanPoint = ScanPointEntity(
                        sessionId = sessionId,
                        sequenceIndex = sequenceCounter++,
                        latitude = geoPoint.latitude,
                        longitude = geoPoint.longitude,
                        altitude = geoPoint.altitude,
                        gpsAccuracyMeters = geoPoint.accuracyMeters,
                        magneticTotalMicroTesla = magSample?.totalMicroTesla ?: 0f,
                        magneticX = magSample?.x ?: 0f,
                        magneticY = magSample?.y ?: 0f,
                        magneticZ = magSample?.z ?: 0f,
                        headingDegrees = heading,
                        timestampMillis = geoPoint.timestampMillis
                    )
                    scanPointDao.insert(scanPoint)
                    _uiState.update { it.copy(recordedPointCount = sequenceCounter) }
                }
        }
    }

    fun pause() {
        if (_uiState.value.state != WalkScanState.RUNNING) return
        _uiState.update { it.copy(state = WalkScanState.PAUSED) }
        // Sensör akışları açık bırakılır (latestHeading/latestMagneticSample güncel kalsın),
        // ancak locationJob içindeki collect bloğu state kontrolüyle kayıt yapmayı durdurur.
    }

    fun resume() {
        if (_uiState.value.state != WalkScanState.PAUSED) return
        _uiState.update { it.copy(state = WalkScanState.RUNNING) }
    }

    fun stop() {
        if (_uiState.value.state == WalkScanState.IDLE) return

        locationJob?.cancel()
        magnetometerJob?.cancel()
        headingJob?.cancel()

        viewModelScope.launch {
            // GPS Temizleme: sıçramalar, hatalı noktalar, outlier, yakın nokta birleştirme
            val cleaned = gpsOutlierFilter.clean(rawRoutePoints)
            val deZigzagged = gpsOutlierFilter.removeZigzagNoise(cleaned)

            val polygon = polygonBuilder.buildFromRoute(deZigzagged)
            val sessionId = _uiState.value.sessionId

            if (polygon != null && sessionId != null) {
                val area = polygon.areaSquareMeters()
                val perimeter = polygon.perimeterMeters()

                val polygonWkt = deZigzagged.joinToString(";") { "${it.latitude},${it.longitude}" }

                val existing = scanSessionDao.getById(sessionId)
                existing?.let {
                    scanSessionDao.update(
                        it.copy(
                            endedAtMillis = System.currentTimeMillis(),
                            polygonWkt = polygonWkt,
                            areaSquareMeters = area,
                            perimeterMeters = perimeter,
                            status = ScanSessionStatus.STOPPED.name
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        state = WalkScanState.STOPPED,
                        polygon = polygon,
                        areaSquareMeters = area,
                        perimeterMeters = perimeter
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        state = WalkScanState.STOPPED,
                        errorMessage = "Polygon oluşturulamadı: en az 3 geçerli GPS noktası gerekli."
                    )
                }
            }
        }
    }

    fun clear() {
        locationJob?.cancel()
        magnetometerJob?.cancel()
        headingJob?.cancel()
        rawRoutePoints.clear()
        sequenceCounter = 0
        latestMagneticSample = null
        latestHeading = null
        _uiState.value = WalkScanUiState()
    }

    override fun onCleared() {
        super.onCleared()
        locationJob?.cancel()
        magnetometerJob?.cancel()
        headingJob?.cancel()
    }
}
