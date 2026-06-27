package com.arkeoscan.phone.ui.screens.heatmap

import android.opengl.GLSurfaceView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.arkeoscan.core.renderer.gl.CameraController
import com.arkeoscan.core.renderer.gl.HeatmapRenderer
import com.arkeoscan.phone.ui.screens.arealysis.AreaAnalysisViewModel

/**
 * Heatmap, Contour ve Surface3D ekranları, son çalıştırılmış analizin grid'ini
 * AreaAnalysisViewModel üzerinden (Activity/Nav-graph scoped hiltViewModel ile
 * aynı instance paylaşılır) okur. Bu sayede analiz tek bir yerde çalışır,
 * sonuçlar tüm görselleştirme ekranlarına yansır.
 */
@Composable
fun HeatmapScreen(
    sharedAnalysisViewModel: AreaAnalysisViewModel
) {
    val uiState by sharedAnalysisViewModel.uiState.collectAsState()
    val cameraController = remember { CameraController() }
    val renderer = remember { HeatmapRenderer(cameraController) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Heatmap") }) },
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
                renderer.updateGrid(grid, colorScale = com.arkeoscan.core.common.model.ColorScale.VIRIDIS)
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
                            cameraController.pan(-pan.x * 0.01f, pan.y * 0.01f)
                            cameraController.zoom(zoom)
                        }
                    }
            )
        }
    }
}
