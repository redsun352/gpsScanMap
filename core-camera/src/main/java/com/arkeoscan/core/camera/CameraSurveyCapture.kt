package com.arkeoscan.core.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Camera Survey ekranı için CameraX sarmalayıcısı.
 *
 * NOT (ÖNEMLİ KISIT): Bu sınıf yalnızca yüzey fotoğrafı yakalar. Çekilen fotoğraflar
 * "yüzey analizine uygun modüler yapı" kapsamında (renk/doku/bitki örtüsü farklılığı
 * gibi yüzeysel ipuçları) kullanılabilir. Kamera hiçbir şekilde yeraltını GPR benzeri
 * doğrudan görüntülediği iddiasıyla kullanılmaz ve böyle bir özellik içermez.
 */
@Singleton
class CameraSurveyCapture @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    suspend fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val provider = getCameraProvider()
        cameraProvider = provider

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        imageCapture = capture

        val selector = CameraSelector.DEFAULT_BACK_CAMERA

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
    }

    fun unbind() {
        cameraProvider?.unbindAll()
    }

    /**
     * Fotoğrafı uygulamanın özel dosya dizinine kaydeder.
     * GPS/EXIF etiketleme bu fonksiyondan sonra ExifGpsWriter ile yapılır
     * (konum bilgisi LocationTracker'dan ayrıca alınıp enjekte edilir).
     */
    suspend fun captureToFile(sessionId: Long): File {
        val capture = imageCapture ?: throw IllegalStateException("Kamera henüz bağlanmadı (bindPreview çağrılmalı)")

        val outputDir = File(context.getExternalFilesDir(null), "camera_survey/session_$sessionId").apply {
            if (!exists()) mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(java.util.Date())
        val outputFile = File(outputDir, "ARKEO_$timestamp.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        return suspendCancellableCoroutine { cont ->
            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        cont.resume(outputFile)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cont.resumeWithException(exception)
                    }
                }
            )
        }
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                try {
                    cont.resume(future.get())
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}
