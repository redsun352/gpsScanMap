package com.arkeoscan.phone

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.arkeoscan.phone.navigation.ArkeoScanNavGraph
import com.arkeoscan.phone.ui.theme.ArkeoScanPhoneTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CAMERA
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Sonuçlar her ekranın kendi runtime kontrolüyle ele alınır (örn. LocationTracker
            çağrıldığında sistem zaten izin ister); burada sadece uygulama açılışında
            toplu olarak önceden istenir ki sahada akış kesilmesin. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher.launch(requiredPermissions)

        setContent {
            ArkeoScanPhoneTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ArkeoScanNavGraph()
                }
            }
        }
    }
}
