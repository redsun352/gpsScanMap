package com.arkeoscan.phone.ui.screens.surface3d

import android.opengl.GLSurfaceView
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arkeoscan.core.common.model.ColorScale
import com.arkeoscan.core.renderer.gl.CameraController
import com.arkeoscan.core.renderer.gl.Surface3DRenderer
import com.arkeoscan.phone.ui.screens.arealysis.AreaAnalysisViewModel

/**
 * 3D Surface ekranı.
 * Rotate: tek parmak sürükleme (yaw/pitch)
 * Zoom: pinch
 * Pan: iki parmak sürükleme (transformGestures pan değeri ile)
 *
 * detectTransformGestures hem pan hem zoom hem rotation (Compose'un kendi
 * 2D rotation'ı) verir; burada pan'ı kameranın rotate fonksiyonuna eşliyoruz
 * (tek parmak sürükleme = döndürme hissi versin), zoom'u zoom'a bağlıyoruz.
 * Gerçek "pan" (sahneyi kaydırma) için ikinci bir gesture modu eklenebilir;
 * basitlik için burada tek dokunma şeması tercih edildi.
 */
@Composable
fun Surface3DScreen(
    sharedAnalysisViewModel: AreaAnalysisViewModel
) {
    val uiState by sharedAnalysisViewModel.uiState.collectAsState()
    val cameraController = remember { CameraController() }
    val renderer = remember { Surface3DRenderer(cameraController) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("3D Surface") }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.grid == null) {
                Text(
                    "Önce Area Analysis ekranında \"Analiz Et\" çalıştırın.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Scaffold
            }

            uiState.grid?.let { grid ->
                renderer.updateGrid(grid, colorScale = ColorScale.VIRIDIS, heightScale = 10f)
            }

            AndroidView(
                factory = { context ->
                    GLSurfaceView(context).apply {
                        setEGLContextClientVersion(2)
                        setRenderer(renderer)
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            cameraController.rotate(pan.x * 0.3f, -pan.y * 0.15f)
                            cameraController.zoom(zoom)
                        }
                    }
            )

            Column(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                Button(onClick = { cameraController.reset() }) {
                    Text("Görünümü Sıfırla")
                }
            }
        }
    }
}
