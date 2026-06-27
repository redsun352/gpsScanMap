package com.arkeoscan.phone.ui.screens.camerasurvey

import android.graphics.BitmapFactory
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkeoscan.core.camera.CameraSurveyCapture
import com.arkeoscan.core.camera.exif.ExifGpsTool
import com.arkeoscan.core.camera.surface.SurfaceAnalysisEngine
import com.arkeoscan.core.common.model.SurfaceAnalysisResult
import com.arkeoscan.core.database.dao.CameraSurveyPhotoDao
import com.arkeoscan.core.database.dao.ScanSessionDao
import com.arkeoscan.core.database.dao.SurfaceAnalysisResultDao
import com.arkeoscan.core.database.entity.CameraSurveyPhotoEntity
import com.arkeoscan.core.database.entity.SurfaceAnalysisResultEntity
import com.arkeoscan.core.gps.tracker.LocationTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class CameraSurveyUiState(
    val isCapturing: Boolean = false,
    val lastCapturedFile: File? = null,
    val capturedCount: Int = 0,
    val lastAnalysisResult: SurfaceAnalysisResult? = null,
    val errorMessage: String? = null
)

/**
 * Camera Survey ViewModel.
 *
 * NOT (ÖNEMLİ KISIT): Kamera burada sadece yüzey fotoğrafı yakalamak ve GPS ile
 * ilişkilendirmek için kullanılır. Hiçbir şekilde yeraltının görüntülendiği
 * iddia edilmez; bu modül GPR benzeri bir yetenek sağlamaz. Fotoğraf çekildikten
 * sonra SurfaceAnalysisEngine, SADECE görünür yüzeydeki renk/doku/bitki örtüsü
 * kontrastını analiz eder (bkz. SurfaceAnalysisResult.DISCLAIMER).
 */
@HiltViewModel
class CameraSurveyViewModel @Inject constructor(
    private val cameraSurveyCapture: CameraSurveyCapture,
    private val exifGpsTool: ExifGpsTool,
    private val locationTracker: LocationTracker,
    private val cameraSurveyPhotoDao: CameraSurveyPhotoDao,
    private val scanSessionDao: ScanSessionDao,
    private val surfaceAnalysisEngine: SurfaceAnalysisEngine,
    private val surfaceAnalysisResultDao: SurfaceAnalysisResultDao
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
                // NOT: aktif oturum yoksa sessionId null kalır (0L gibi var olmayan
                // bir id KULLANILMAZ — bu, foreign key kısıtını ihlal edip
                // SQLITE_CONSTRAINT_FOREIGNKEY hatası verirdi). Camera Survey,
                // Walk Scan oturumu olmadan da bağımsız çalışabilir.
                val sessionId = activeSession?.id

                val file = cameraSurveyCapture.captureToFile(sessionId ?: 0L)
                val location = locationTracker.getLastKnownLocation()

                location?.let { exifGpsTool.writeGps(file, it) }

                val photoId = cameraSurveyPhotoDao.insert(
                    CameraSurveyPhotoEntity(
                        sessionId = sessionId,
                        filePath = file.absolutePath,
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        capturedAtMillis = System.currentTimeMillis()
                    )
                )

                // Yüzey analizi: bitmap decode + piksel taraması CPU-yoğun olduğundan
                // Dispatchers.Default üzerinde (ana thread'i bloklamadan) çalıştırılır.
                val analysisResult = withContext(Dispatchers.Default) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    bitmap?.let { bmp ->
                        val result = surfaceAnalysisEngine.analyze(photoId, bmp)
                        bmp.recycle()
                        result
                    }
                }

                analysisResult?.let { result ->
                    surfaceAnalysisResultDao.insert(
                        SurfaceAnalysisResultEntity(
                            photoId = result.photoId,
                            meanRed = result.colorProfile.meanRed,
                            meanGreen = result.colorProfile.meanGreen,
                            meanBlue = result.colorProfile.meanBlue,
                            brightnessVariance = result.colorProfile.brightnessVariance,
                            vegetationIndex = result.vegetationIndex,
                            textureRoughness = result.textureRoughness,
                            noteworthySurfacePatch = result.noteworthySurfacePatch,
                            noteReasons = result.noteReasons.joinToString(",") { it.name },
                            analyzedAtMillis = result.analyzedAtMillis
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        isCapturing = false,
                        lastCapturedFile = file,
                        lastAnalysisResult = analysisResult,
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
