package com.gamerx.manager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GamerXRed,
    secondary = GamerXRedLight,
    tertiary = SuccessColor,
    background = Black,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariant,
    onPrimary = White,
    onSecondary = Black,
    onTertiary = Black,
    onBackground = White,
    onSurface = White,
    error = ErrorColor
)

// We enforce dark theme for GamerX Manager
private val LightColorScheme = DarkColorScheme

@Composable
fun GamerXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val primary = ThemeManager.accentColor.value
    
    val colorScheme = darkColorScheme(
        primary = primary,
        secondary = primary.copy(alpha = 0.7f),
        tertiary = SuccessColor,
        background = Black,
        surface = SurfaceDark,
        surfaceVariant = SurfaceVariant,
        onPrimary = White,
        onSecondary = Black,
        onTertiary = Black,
        onBackground = White,
        onSurface = White,
        error = ErrorColor
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
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
