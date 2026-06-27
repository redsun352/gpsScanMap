package com.arkeoscan.phone.ui.screens.camerasurvey

import android.widget.FrameLayout
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.arkeoscan.core.common.model.SurfaceAnalysisResult
import com.arkeoscan.core.common.model.SurfaceNoteReason

@Composable
fun CameraSurveyScreen(
    viewModel: CameraSurveyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        onDispose { viewModel.unbindPreview() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Camera Survey") }) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.capturePhoto() }) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Çek")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        viewModel.bindPreview(lifecycleOwner, this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${uiState.capturedCount} fotoğraf çekildi",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
                uiState.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                uiState.lastAnalysisResult?.let { result ->
                    SurfaceAnalysisCard(result)
                }
            }
        }
    }
}

@Composable
private fun SurfaceAnalysisCard(result: SurfaceAnalysisResult) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (result.noteworthySurfacePatch) "İncelemeye değer yüzey yaması" else "Yüzey analizi tamamlandı",
                style = MaterialTheme.typography.titleMedium,
                color = if (result.noteworthySurfacePatch) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                "Bitki örtüsü indeksi (VARI): %.2f".format(result.vegetationIndex),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Doku pürüzlülüğü: %.1f".format(result.textureRoughness),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (result.noteReasons.isNotEmpty()) {
                Text(
                    "Nedenler: ${result.noteReasons.joinToString(", ") { reasonLabel(it) }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                SurfaceAnalysisResult.DISCLAIMER,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun reasonLabel(reason: SurfaceNoteReason): String = when (reason) {
    SurfaceNoteReason.VEGETATION_CONTRAST -> "bitki örtüsü farkı"
    SurfaceNoteReason.COLOR_CONTRAST -> "renk/ton farkı"
    SurfaceNoteReason.TEXTURE_CONTRAST -> "doku farkı"
}
