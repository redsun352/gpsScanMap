package com.arkeoscan.phone.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arkeoscan.core.common.model.ColorScale
import com.arkeoscan.core.common.model.DistanceUnit
import com.arkeoscan.core.common.model.GpsAccuracyMode
import com.arkeoscan.core.common.model.GridResolution

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = "GPS Hassasiyeti") {
                GpsAccuracyMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.gpsAccuracyMode == mode,
                        onClick = { viewModel.setGpsAccuracyMode(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }

            SettingsSection(title = "Grid Çözünürlüğü") {
                GridResolution.entries.forEach { resolution ->
                    FilterChip(
                        selected = settings.gridResolution == resolution,
                        onClick = { viewModel.setGridResolution(resolution) },
                        label = { Text(resolution.label) }
                    )
                }
            }

            SettingsSection(title = "Renk Skalası") {
                ColorScale.entries.forEach { scale ->
                    FilterChip(
                        selected = settings.colorScale == scale,
                        onClick = { viewModel.setColorScale(scale) },
                        label = { Text(scale.name) }
                    )
                }
            }

            SettingsSection(title = "Birimler") {
                DistanceUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = settings.distanceUnit == unit,
                        onClick = { viewModel.setDistanceUnit(unit) },
                        label = { Text(if (unit == DistanceUnit.METERS) "Metre" else "Feet") }
                    )
                }
            }

            SettingsSection(title = "Tema") {
                Text(
                    "Karanlık tema (sabit)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}
