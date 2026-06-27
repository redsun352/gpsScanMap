package com.arkeoscan.phone.ui.screens.livemag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkeoscan.core.common.model.MagnetometerBaseline
import com.arkeoscan.core.common.model.MagnetometerSample
import com.arkeoscan.core.database.dao.MagnetometerCalibrationDao
import com.arkeoscan.core.database.entity.MagnetometerCalibrationEntity
import com.arkeoscan.core.magnetometer.MagnetometerSensorManager
import com.arkeoscan.core.magnetometer.calibration.MagnetometerCalibrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CalibrationState { IDLE, CALIBRATING, CALIBRATED }

data class LiveMagnetometerUiState(
    val recentSamples: List<MagnetometerSample> = emptyList(), // canlı grafik için kayan pencere
    val currentTotal: Float = 0f,
    val baseline: MagnetometerBaseline? = null,
    val deviationSigma: Float = 0f,
    val noiseFloor: Float = 0f,
    val calibrationState: CalibrationState = CalibrationState.IDLE,
    val calibrationProgress: Float = 0f
)

@HiltViewModel
class LiveMagnetometerViewModel @Inject constructor(
    private val sensorManager: MagnetometerSensorManager,
    private val calibrator: MagnetometerCalibrator,
    private val calibrationDao: MagnetometerCalibrationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveMagnetometerUiState())
    val uiState: StateFlow<LiveMagnetometerUiState> = _uiState.asStateFlow()

    private var collectJob: Job? = null
    private val graphWindow = ArrayDeque<MagnetometerSample>()
    private val maxGraphPoints = 120 // ~ son birkaç dakikalık canlı grafik penceresi

    private val calibrationBuffer = mutableListOf<MagnetometerSample>()
    private var isCalibrating = false
    private val calibrationDurationMillis = 5000L
    private var calibrationStartTime = 0L

    init {
        loadLatestCalibration()
        startListening()
    }

    private fun loadLatestCalibration() {
        viewModelScope.launch {
            val latest = calibrationDao.getLatest()
            latest?.let {
                _uiState.update {
                    state -> state.copy(
                        baseline = MagnetometerBaseline(
                            meanMicroTesla = it.meanMicroTesla,
                            stdDevMicroTesla = it.stdDevMicroTesla,
                            minMicroTesla = it.minMicroTesla,
                            maxMicroTesla = it.maxMicroTesla,
                            sampleCount = it.sampleCount
                        ),
                        calibrationState = CalibrationState.CALIBRATED
                    )
                }
            }
        }
    }

    private fun startListening() {
        collectJob = viewModelScope.launch {
            sensorManager.sampleFlow().collect { sample ->
                graphWindow.addLast(sample)
                if (graphWindow.size > maxGraphPoints) graphWindow.removeFirst()

                val noiseFloor = calibrator.peakToPeakNoiseFloor(graphWindow.toList())
                val baseline = _uiState.value.baseline
                val sigma = baseline?.deviationSigma(sample.totalMicroTesla) ?: 0f

                _uiState.update {
                    it.copy(
                        recentSamples = graphWindow.toList(),
                        currentTotal = sample.totalMicroTesla,
                        deviationSigma = sigma,
                        noiseFloor = noiseFloor
                    )
                }

                if (isCalibrating) {
                    calibrationBuffer.add(sample)
                    val elapsed = System.currentTimeMillis() - calibrationStartTime
                    val progress = (elapsed.toFloat() / calibrationDurationMillis).coerceIn(0f, 1f)
                    _uiState.update { it.copy(calibrationProgress = progress) }

                    if (elapsed >= calibrationDurationMillis) {
                        finishCalibration()
                    }
                }
            }
        }
    }

    fun startCalibration() {
        isCalibrating = true
        calibrationBuffer.clear()
        calibrationStartTime = System.currentTimeMillis()
        _uiState.update { it.copy(calibrationState = CalibrationState.CALIBRATING, calibrationProgress = 0f) }
    }

    private fun finishCalibration() {
        isCalibrating = false
        val baseline = calibrator.computeBaseline(calibrationBuffer)

        if (baseline != null) {
            viewModelScope.launch {
                calibrationDao.insert(
                    MagnetometerCalibrationEntity(
                        meanMicroTesla = baseline.meanMicroTesla,
                        stdDevMicroTesla = baseline.stdDevMicroTesla,
                        minMicroTesla = baseline.minMicroTesla,
                        maxMicroTesla = baseline.maxMicroTesla,
                        sampleCount = baseline.sampleCount,
                        calibratedAtMillis = System.currentTimeMillis()
                    )
                )
            }
            _uiState.update {
                it.copy(
                    baseline = baseline,
                    calibrationState = CalibrationState.CALIBRATED,
                    calibrationProgress = 1f
                )
            }
        } else {
            _uiState.update { it.copy(calibrationState = CalibrationState.IDLE) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        collectJob?.cancel()
    }
}
