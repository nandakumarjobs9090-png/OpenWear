package com.openwear.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = PurplePrimary,
    onSecondary = White,
    secondaryContainer = Purple80,
    onSecondaryContainer = Purple10,
    tertiary = CoralPrimary,
    onTertiary = White,
    tertiaryContainer = Coral80,
    onTertiaryContainer = Coral40,
    background = SurfaceLight,
    onBackground = Grey10,
    surface = SurfaceCard,
    onSurface = Grey10,
    surfaceVariant = Grey95,
    onSurfaceVariant = Grey40,
    outline = Grey80,
    error = CoralPrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Purple80,
    onSecondary = Purple20,
    secondaryContainer = Purple40,
    onSecondaryContainer = Purple80,
    tertiary = Coral80,
    onTertiary = Coral40,
    tertiaryContainer = Coral40,
    onTertiaryContainer = Coral80,
    background = BackgroundDark,
    onBackground = Grey90,
    surface = SurfaceCardDark,
    onSurface = Grey90,
    surfaceVariant = Grey20,
    onSurfaceVariant = Grey80,
    outline = Grey30,
    error = Coral80,
)

@Composable
fun OpenWearTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

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
