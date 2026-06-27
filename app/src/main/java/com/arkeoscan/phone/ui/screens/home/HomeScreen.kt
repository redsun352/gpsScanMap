package com.arkeoscan.phone.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MultilineChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.arkeoscan.phone.navigation.ArkeoScanDestination

private data class MenuItem(
    val destination: ArkeoScanDestination,
    val icon: ImageVector,
    val subtitleTr: String
)

private val menuItems = listOf(
    MenuItem(ArkeoScanDestination.WalkScan, Icons.Filled.DirectionsWalk, "Sahada yürüyerek tara"),
    MenuItem(ArkeoScanDestination.LiveMagnetometer, Icons.Filled.Sensors, "Canlı manyetometre verisi"),
    MenuItem(ArkeoScanDestination.CameraSurvey, Icons.Filled.CameraAlt, "Yüzey fotoğrafı + GPS"),
    MenuItem(ArkeoScanDestination.AreaAnalysis, Icons.Filled.Map, "Polygon ve alan analizi"),
    MenuItem(ArkeoScanDestination.Heatmap, Icons.Filled.Texture, "Manyetik yoğunluk haritası"),
    MenuItem(ArkeoScanDestination.Contour, Icons.Filled.MultilineChart, "İzoline / kontur görünümü"),
    MenuItem(ArkeoScanDestination.Surface3D, Icons.Filled.Terrain, "3D yüzey görselleştirme"),
    MenuItem(ArkeoScanDestination.Reports, Icons.Filled.Description, "PDF / CSV / GeoJSON / KML"),
    MenuItem(ArkeoScanDestination.Settings, Icons.Filled.Settings, "Hassasiyet, grid, tema")
)

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ArkeoScan Phone", style = MaterialTheme.typography.titleLarge) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp).let {
                PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 16.dp
                )
            },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(menuItems) { item ->
                MenuCard(item = item, onClick = { onNavigate(item.destination.route) })
            }
        }
    }
}

@Composable
private fun MenuCard(item: MenuItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.destination.titleTr,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = item.destination.titleTr,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.subtitleTr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
