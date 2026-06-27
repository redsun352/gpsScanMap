package com.arkeoscan.phone.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reports") }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Aktif oturum için PDF, CSV, GeoJSON ve KML dosyaları oluşturulur.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = viewModel::exportLatestSession,
                enabled = !uiState.isExporting,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Raporu Oluştur ve Dışa Aktar")
                }
            }

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            uiState.exportedFiles?.let { files ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Oluşturulan Dosyalar", style = MaterialTheme.typography.titleMedium)
                        FileRow("PDF Rapor", files.pdf.absolutePath)
                        FileRow("CSV (Ölçüm Noktaları)", files.csvScanPoints.absolutePath)
                        FileRow("CSV (Anomaliler)", files.csvAnomalies.absolutePath)
                        files.geoJsonPolygon?.let { FileRow("GeoJSON (Polygon)", it.absolutePath) }
                        files.geoJsonAnomalies?.let { FileRow("GeoJSON (Anomaliler)", it.absolutePath) }
                        files.kml?.let { FileRow("KML", it.absolutePath) }
                        files.png?.let { FileRow("PNG", it.absolutePath) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(label: String, path: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(path, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
