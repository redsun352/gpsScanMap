package com.arkeoscan.phone.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arkeoscan.phone.ui.screens.arealysis.AreaAnalysisScreen
import com.arkeoscan.phone.ui.screens.arealysis.AreaAnalysisViewModel
import com.arkeoscan.phone.ui.screens.camerasurvey.CameraSurveyScreen
import com.arkeoscan.phone.ui.screens.contour.ContourScreen
import com.arkeoscan.phone.ui.screens.heatmap.HeatmapScreen
import com.arkeoscan.phone.ui.screens.home.HomeScreen
import com.arkeoscan.phone.ui.screens.livemag.LiveMagnetometerScreen
import com.arkeoscan.phone.ui.screens.reports.ReportsScreen
import com.arkeoscan.phone.ui.screens.settings.SettingsScreen
import com.arkeoscan.phone.ui.screens.surface3d.Surface3DScreen
import com.arkeoscan.phone.ui.screens.walkscan.WalkScanScreen

/**
 * AreaAnalysisViewModel, Heatmap/Contour/Surface3D ekranları arasında AYNI grid/anomali
 * sonucunu paylaşmak için Activity scope'unda alınır (hiltViewModel(activityContext)).
 * Bu sayede "Area Analysis" ekranında bir kez "Analiz Et" çalıştırılır, sonuç tüm
 * görselleştirme ekranlarına otomatik yansır.
 */
@Composable
fun ArkeoScanNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val activity = LocalContext.current as ComponentActivity
    val sharedAnalysisViewModel: AreaAnalysisViewModel = hiltViewModel(activity)

    NavHost(
        navController = navController,
        startDestination = ArkeoScanDestination.Home.route
    ) {
        composable(ArkeoScanDestination.Home.route) {
            HomeScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(ArkeoScanDestination.WalkScan.route) {
            WalkScanScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(ArkeoScanDestination.LiveMagnetometer.route) {
            LiveMagnetometerScreen()
        }
        composable(ArkeoScanDestination.CameraSurvey.route) {
            CameraSurveyScreen()
        }
        composable(ArkeoScanDestination.AreaAnalysis.route) {
            AreaAnalysisScreen(viewModel = sharedAnalysisViewModel)
        }
        composable(ArkeoScanDestination.Heatmap.route) {
            HeatmapScreen(sharedAnalysisViewModel = sharedAnalysisViewModel)
        }
        composable(ArkeoScanDestination.Contour.route) {
            ContourScreen(sharedAnalysisViewModel = sharedAnalysisViewModel)
        }
        composable(ArkeoScanDestination.Surface3D.route) {
            Surface3DScreen(sharedAnalysisViewModel = sharedAnalysisViewModel)
        }
        composable(ArkeoScanDestination.Reports.route) {
            ReportsScreen()
        }
        composable(ArkeoScanDestination.Settings.route) {
            SettingsScreen()
        }
    }
}
