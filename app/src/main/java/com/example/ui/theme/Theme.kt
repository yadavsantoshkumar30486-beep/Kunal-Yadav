package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = RadarCyan,
    secondary = RadarAmber,
    tertiary = RadarWarningRed,
    background = RadarDarkBackground,
    surface = RadarTerminalCard,
    onPrimary = RadarDarkBackground,
    onSecondary = RadarDarkBackground,
    onTertiary = RadarDarkBackground,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White
  )

private val LightColorScheme = DarkColorScheme // Force atmospheric dark theme always for spectacular aesthetics

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}
