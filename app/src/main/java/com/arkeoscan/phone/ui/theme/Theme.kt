package com.arkeoscan.phone.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ArkeoDarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = BackgroundPrimary,
    secondary = AccentSecondary,
    onSecondary = BackgroundPrimary,
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardElevated,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    outline = DividerColor
)

/**
 * "Modern siyah tema" gereksinimi — uygulama her zaman karanlık tema kullanır
 * (Settings ekranındaki darkTheme bayrağı ileride açık tema desteği eklemek
 * isteyenler için altyapı olarak bırakılmıştır, şu an varsayılan davranışı değiştirmez).
 */
@Composable
fun ArkeoScanPhoneTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = ArkeoDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ArkeoTypography,
        content = content
    )
}
