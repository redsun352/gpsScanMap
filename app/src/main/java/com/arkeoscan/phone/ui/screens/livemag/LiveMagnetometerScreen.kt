package com.arkeoscan.phone.ui.screens.livemag

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arkeoscan.core.common.model.MagnetometerSample

@Composable
fun LiveMagnetometerScreen(
    viewModel: LiveMagnetometerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Live Magnetometer") }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ReadingCard(
                currentTotal = uiState.currentTotal,
                deviationSigma = uiState.deviationSigma,
                noiseFloor = uiState.noiseFloor
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().height(220.dp)
            ) {
                LiveGraph(
                    samples = uiState.recentSamples,
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                )
            }

            CalibrationCard(
                state = uiState.calibrationState,
                progress = uiState.calibrationProgress,
                baselineMean = uiState.baseline?.meanMicroTesla,
                baselineStdDev = uiState.baseline?.stdDevMicroTesla,
                onStartCalibration = viewModel::startCalibration
            )
        }
    }
}

@Composable
private fun ReadingCard(currentTotal: Float, deviationSigma: Float, noiseFloor: Float) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "%.2f µT".format(currentTotal),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Sapma: %.2fσ".format(deviationSigma),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Gürültü tabanı: %.2f µT".format(noiseFloor),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LiveGraph(samples: List<MagnetometerSample>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (samples.size < 2) return@Canvas

        val values = samples.map { it.totalMicroTesla }
        val minV = values.min()
        val maxV = values.max()
        val range = (maxV - minV).let { if (it < 1f) 1f else it }

        val stepX = size.width / (samples.size - 1).coerceAtLeast(1)

        var previousOffset: Offset? = null
        for (i in values.indices) {
            val normalized = (values[i] - minV) / range
            val x = i * stepX
            val y = size.height - (normalized * size.height)
            val current = Offset(x, y)
            previousOffset?.let { prev ->
                drawLine(
                    color = lineColor,
                    start = prev,
                    end = current,
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
            previousOffset = current
        }
    }
}

@Composable
private fun CalibrationCard(
    state: CalibrationState,
    progress: Float,
    baselineMean: Float?,
    baselineStdDev: Float?,
    onStartCalibration: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Kalibrasyon", style = MaterialTheme.typography.titleMedium)
            Text(
                "Cihazı sabit tutun veya 8 şeklinde hareket ettirin. 5 saniye sürer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state == CalibrationState.CALIBRATING) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            baselineMean?.let { mean ->
                Text(
                    "Taban: %.2f µT ± %.2f".format(mean, baselineStdDev ?: 0f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = onStartCalibration,
                enabled = state != CalibrationState.CALIBRATING
            ) {
                Text(if (state == CalibrationState.CALIBRATING) "Kalibre ediliyor..." else "Kalibrasyonu Başlat")
            }
        }
    }
}
