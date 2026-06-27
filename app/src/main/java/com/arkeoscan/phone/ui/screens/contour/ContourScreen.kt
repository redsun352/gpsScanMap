package com.arkeoscan.phone.ui.screens.contour

import android.opengl.GLSurfaceView
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
import com.arkeoscan.core.common.model.ColorScale
import com.arkeoscan.core.renderer.gl.CameraController
import com.arkeoscan.core.renderer.gl.ContourRenderer
import com.arkeoscan.phone.ui.screens.arealysis.AreaAnalysisViewModel

@Composable
fun ContourScreen(
    sharedAnalysisViewModel: AreaAnalysisViewModel
) {
    val uiState by sharedAnalysisViewModel.uiState.collectAsState()
    val cameraController = remember { CameraController() }
    val renderer = remember { ContourRenderer(cameraController) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Contour") }) },
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
                renderer.updateGrid(grid, colorScale = ColorScale.SURFER_CLASSIC)
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
