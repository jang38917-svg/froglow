package com.froglow.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = FroglowPrimary,
    background = FroglowBg,
    surface = FroglowSurface,
    surfaceVariant = FroglowSurfaceSoft,
    onPrimary = White,
    onBackground = FroglowText,
    onSurface = FroglowText,
    onSurfaceVariant = FroglowTextSub
)

private val LightColorScheme = lightColorScheme(
    primary = FroglowPrimary,
    background = FroglowBg,
    surface = FroglowSurface,
    surfaceVariant = FroglowSurfaceSoft,
    onPrimary = White,
    onBackground = FroglowText,
    onSurface = FroglowText,
    onSurfaceVariant = FroglowTextSub
)

@Composable
fun FroglowTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = true
            insetsController.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}