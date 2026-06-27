package com.arkeoscan.phone.navigation

sealed class ArkeoScanDestination(val route: String, val titleTr: String) {
    data object Home : ArkeoScanDestination("home", "Ana Menü")
    data object WalkScan : ArkeoScanDestination("walk_scan", "Walk Scan")
    data object LiveMagnetometer : ArkeoScanDestination("live_magnetometer", "Live Magnetometer")
    data object CameraSurvey : ArkeoScanDestination("camera_survey", "Camera Survey")
    data object AreaAnalysis : ArkeoScanDestination("area_analysis", "Area Analysis")
    data object Heatmap : ArkeoScanDestination("heatmap", "Heatmap")
    data object Contour : ArkeoScanDestination("contour", "Contour")
    data object Surface3D : ArkeoScanDestination("surface_3d", "3D Surface")
    data object Reports : ArkeoScanDestination("reports", "Reports")
    data object Settings : ArkeoScanDestination("settings", "Settings")

    companion object {
        val bottomLevelDestinations = listOf(Home, WalkScan, AreaAnalysis, Reports, Settings)
    }
}
