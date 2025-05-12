package com.example.purrytify.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark color scheme for Purrytify (most users will use this)
private val PurrytifyDarkColorScheme = darkColorScheme(
    primary = PurrytifyGreen,
    secondary = PurrytifyLightGray,
    tertiary = PurrytifyDarkGray,
    background = PurrytifyBlack,
    surface = PurrytifyLighterBlack,
    onPrimary = PurrytifyWhite,
    onSecondary = PurrytifyWhite,
    onTertiary = PurrytifyWhite,
    onBackground = PurrytifyWhite,
    onSurface = PurrytifyWhite,
    error = PurritifyRed,
    onError = PurrytifyWhite
)

// Light color scheme for Purrytify (fallback)
private val PurrytifyLightColorScheme = lightColorScheme(
    primary = PurrytifyGreen,
    secondary = PurrytifyDarkGray,
    tertiary = PurrytifyLightGray,
    background = PurrytifyWhite,
    surface = Color(0xFFF8F8F8),
    onPrimary = PurrytifyWhite,
    onSecondary = PurrytifyWhite,
    onTertiary = PurrytifyBlack,
    onBackground = PurrytifyBlack,
    onSurface = PurrytifyBlack,
    error = PurritifyRed,
    onError = PurrytifyWhite
)

@Composable
fun PurrytifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set default to false to use our branding colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> PurrytifyDarkColorScheme
        else -> PurrytifyLightColorScheme
    }
    
    // Apply edge-to-edge design
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}