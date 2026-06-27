package com.arkeoscan.phone.ui.screens.walkscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun WalkScanScreen(
    onNavigateBack: () -> Unit,
    viewModel: WalkScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val routeLatLng = uiState.routePoints.map { LatLng(it.latitude, it.longitude) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            routeLatLng.lastOrNull() ?: LatLng(38.514, 35.786), // Kayseri varsayılan
            17f
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Walk Scan") }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            GoogleMap(
                modifier = Modifier.fillMaxWidth().weight(1f),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = MapType.HYBRID)
            ) {
                if (routeLatLng.size >= 2) {
                    Polyline(
                        points = routeLatLng,
                        color = MaterialTheme.colorScheme.primary,
                        width = 6f
                    )
                }
                uiState.polygon?.let { polygon ->
                    val polygonLatLng = polygon.vertices.map { LatLng(it.latitude, it.longitude) }
                    Polygon(
                        points = polygonLatLng,
                        strokeColor = MaterialTheme.colorScheme.secondary,
                        fillColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                        strokeWidth = 4f
                    )
                }
            }

            StatusCard(uiState)

            ControlButtons(
                state = uiState.state,
                onStart = viewModel::start,
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onStop = viewModel::stop,
                onClear = viewModel::clear
            )
        }
    }
}

@Composable
private fun StatusCard(uiState: WalkScanUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Durum: ${stateLabel(uiState.state)}", style = MaterialTheme.typography.labelLarge)
                Text("${uiState.recordedPointCount} nokta", style = MaterialTheme.typography.labelLarge)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Manyetik: %.1f µT".format(uiState.currentMagneticTotal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    uiState.currentHeading?.let { "Yön: %.0f°".format(it) } ?: "Yön: --",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            uiState.areaSquareMeters?.let { area ->
                Text(
                    "Alan: %.1f m²  •  Çevre: %.1f m".format(area, uiState.perimeterMeters ?: 0.0),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            uiState.errorMessage?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ControlButtons(
    state: WalkScanState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (state) {
            WalkScanState.IDLE, WalkScanState.STOPPED -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text(" START")
                }
            }
            WalkScanState.RUNNING -> {
                OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Filled.Pause, contentDescription = null)
                    Text(" PAUSE")
                }
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Text(" STOP")
                }
            }
            WalkScanState.PAUSED -> {
                Button(
                    onClick = onResume,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text(" DEVAM ET")
                }
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Text(" STOP")
                }
            }
        }
        OutlinedButton(onClick = onClear, modifier = Modifier.height(52.dp)) {
            Icon(Icons.Filled.Clear, contentDescription = "Clear")
        }
    }
}

private fun stateLabel(state: WalkScanState): String = when (state) {
    WalkScanState.IDLE -> "Bekliyor"
    WalkScanState.RUNNING -> "Taranıyor"
    WalkScanState.PAUSED -> "Duraklatıldı"
    WalkScanState.STOPPED -> "Tamamlandı"
}
