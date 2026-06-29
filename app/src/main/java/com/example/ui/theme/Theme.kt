package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = CharcoalDark,
    primaryContainer = VioletAccent,
    onPrimaryContainer = Color.White,
    secondary = VioletAccent,
    onSecondary = Color.White,
    tertiary = EmeraldSuccess,
    background = CharcoalDark,
    onBackground = TextWhite,
    surface = SurfaceDark,
    onSurface = TextWhite,
    error = CrimsonError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AmberPrimary,
    onPrimary = CharcoalDark,
    primaryContainer = VioletAccent,
    onPrimaryContainer = Color.White,
    secondary = VioletAccent,
    onSecondary = Color.White,
    tertiary = EmeraldSuccess,
    background = Color(0xFFF9F9FB),
    onBackground = Color(0xFF1F2026),
    surface = Color.White,
    onSurface = Color(0xFF1F2026),
    error = CrimsonError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
