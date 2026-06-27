package com.arkeoscan.phone.ui.screens.arealysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arkeoscan.core.common.model.AnomalyResult
import com.arkeoscan.core.common.model.GridResolution

@Composable
fun AreaAnalysisScreen(
    viewModel: AreaAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Bu ekran her görüntülendiğinde (örn. Walk Scan'de yeni tarama yapılıp
    // geri dönüldüğünde) en güncel oturumu yeniden yükler. ViewModel Activity
    // scope'unda paylaşıldığı için init{} bloğu sadece bir kez çalışır;
    // bu yüzden ekran seviyesinde tekrar tetikleme gerekir.
    LaunchedEffect(Unit) {
        viewModel.loadLatestSession()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Area Analysis") }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.polygon == null) {
                Text(
                    "Henüz tamamlanmış bir tarama bulunamadı. Önce Walk Scan ile bir alan tarayın.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Scaffold
            }

            SummaryCard(uiState)
            GridResolutionSelector(
                selected = uiState.gridResolution,
                onSelect = viewModel::setGridResolution
            )

            Button(
                onClick = viewModel::runAnalysis,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Analiz Et (IDW + Anomali Motoru)")
                }
            }

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            if (uiState.anomalies.isNotEmpty()) {
                Text(
                    "Tespit Edilen Anomaliler (${uiState.anomalies.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.anomalies) { anomaly ->
                        AnomalyCard(anomaly)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(uiState: AreaAnalysisUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Tarama Alanı Özeti", style = MaterialTheme.typography.titleMedium)
            Text(
                "Alan: %.1f m²".format(uiState.areaSquareMeters ?: 0.0),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Çevre: %.1f m".format(uiState.perimeterMeters ?: 0.0),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            uiState.grid?.let { grid ->
                Text(
                    "Grid: ${grid.rows}×${grid.cols} hücre, ${grid.resolutionMeters} m çözünürlük",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun GridResolutionSelector(selected: GridResolution, onSelect: (GridResolution) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GridResolution.entries.forEach { resolution ->
            AssistChip(
                onClick = { onSelect(resolution) },
                label = { Text(resolution.label) },
                colors = if (resolution == selected) {
                    androidx.compose.material3.AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                } else {
                    androidx.compose.material3.AssistChipDefaults.assistChipColors()
                }
            )
        }
    }
}

@Composable
private fun AnomalyCard(anomaly: AnomalyResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(anomaly.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Güven: %.0f%%".format(anomaly.confidenceScore * 100),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Sapma: %.2fσ  •  Alan: %.1f m²  •  Şekil: %s".format(
                    anomaly.deviationSigma, anomaly.areaSquareMeters, anomaly.shape.name
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                AnomalyResult.DISCLAIMER,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
