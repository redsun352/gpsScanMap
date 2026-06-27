package com.arkeoscan.phone.ui.screens.camerasurvey

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkeoscan.core.camera.CameraSurveyCapture
import com.arkeoscan.core.camera.exif.ExifGpsTool
import com.arkeoscan.core.database.dao.CameraSurveyPhotoDao
import com.arkeoscan.core.database.dao.ScanSessionDao
import com.arkeoscan.core.database.entity.CameraSurveyPhotoEntity
import com.arkeoscan.core.gps.tracker.LocationTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CameraSurveyUiState(
    val isCapturing: Boolean = false,
    val lastCapturedFile: File? = null,
    val capturedCount: Int = 0,
    val errorMessage: String? = null
)

/**
 * Camera Survey ViewModel.
 *
 * NOT (ÖNEMLİ KISIT): Kamera burada sadece yüzey fotoğrafı yakalamak ve GPS ile
 * ilişkilendirmek için kullanılır. Hiçbir şekilde yeraltının görüntülendiği
 * iddia edilmez; bu modül GPR benzeri bir yetenek sağlamaz.
 */
@HiltViewModel
class CameraSurveyViewModel @Inject constructor(
    private val cameraSurveyCapture: CameraSurveyCapture,
    private val exifGpsTool: ExifGpsTool,
    private val locationTracker: LocationTracker,
    private val cameraSurveyPhotoDao: CameraSurveyPhotoDao,
    private val scanSessionDao: ScanSessionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraSurveyUiState())
    val uiState: StateFlow<CameraSurveyUiState> = _uiState.asStateFlow()

    fun bindPreview(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        viewModelScope.launch {
            try {
                cameraSurveyCapture.bindPreview(lifecycleOwner, previewView)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Kamera başlatılamadı: ${e.message}") }
            }
        }
    }

    fun unbindPreview() {
        cameraSurveyCapture.unbind()
    }

    fun capturePhoto() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = true, errorMessage = null) }
            try {
                val activeSession = scanSessionDao.getActiveSession()
                val sessionId = activeSession?.id ?: 0L

                val file = cameraSurveyCapture.captureToFile(sessionId)
                val location = locationTracker.getLastKnownLocation()

                location?.let { exifGpsTool.writeGps(file, it) }

                cameraSurveyPhotoDao.insert(
                    CameraSurveyPhotoEntity(
                        sessionId = sessionId,
                        filePath = file.absolutePath,
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        capturedAtMillis = System.currentTimeMillis()
                    )
                )

                _uiState.update {
                    it.copy(
                        isCapturing = false,
                        lastCapturedFile = file,
                        capturedCount = it.capturedCount + 1
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isCapturing = false, errorMessage = "Fotoğraf çekilemedi: ${e.message}")
                }
            }
        }
    }
}
