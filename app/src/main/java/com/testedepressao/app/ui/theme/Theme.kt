package com.testedepressao.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CarbonBlack,
    secondary = RaceRed,
    onSecondary = TextPrimary,
    tertiary = PeugeotCopper,
    background = CarbonBlack,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceElevated,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = DarkBorder
)

@Composable
fun TesteDePressaoTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CarbonBlack.toArgb()
            window.navigationBarColor = CarbonBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
