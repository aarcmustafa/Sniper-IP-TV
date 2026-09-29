package com.stitten.stitteniptv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TvColors = darkColorScheme(
    primary = TvPrimary,
    onPrimary = TvText,
    secondary = TvAccent,
    background = TvBackground,
    onBackground = TvText,
    surface = TvSurface,
    onSurface = TvText,
    surfaceVariant = TvSurfaceElevated,
    error = TvError
)

@Composable
fun STTITENIPTVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvColors,
        typography = TvTypography,
        content = content
    )
}
