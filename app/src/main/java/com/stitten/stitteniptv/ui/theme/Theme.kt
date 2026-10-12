package com.stitten.stitteniptv.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════════
//  🎨 Color Scheme — يعتمد على Color.kt كمصدر وحيد
//  Dark دائمًا (تطبيق TV)
// ═══════════════════════════════════════════════════════════════

private val DarkColorScheme = darkColorScheme(
    // ─── الألوان الأساسية ───
    primary = TvPrimary,
    onPrimary = Color.White,
    primaryContainer = TvPrimaryDark,
    onPrimaryContainer = TvText,

    // ─── الألوان الثانوية ───
    secondary = TvPrimaryLight,
    onSecondary = Color.Black,
    secondaryContainer = TvSurfaceElevated,
    onSecondaryContainer = TvText,

    // ─── Tertiary (ذهبي للعناصر المميزة) ───
    tertiary = TvGold,
    onTertiary = Color.Black,
    tertiaryContainer = TvGold.copy(alpha = 0.2f),
    onTertiaryContainer = TvGold,

    // ─── الخلفيات ───
    background = TvBackground,
    onBackground = TvText,

    // ─── الأسطح ───
    surface = TvSurface,
    onSurface = TvText,
    surfaceVariant = TvSurfaceElevated,
    onSurfaceVariant = TvTextSecondary,
    surfaceContainer = TvSurfaceHigh,
    surfaceContainerHigh = TvSurfaceHigh,

    // ─── الأخطاء ───
    error = TvError,
    onError = Color.White,
    errorContainer = TvError.copy(alpha = 0.2f),
    onErrorContainer = TvError,

    // ─── الحدود ───
    outline = TvBorder,
    outlineVariant = TvBorder.copy(alpha = 0.5f),

    // ─── Inverse ───
    inverseSurface = TvText,
    inverseOnSurface = TvBackground,
    inversePrimary = TvPrimaryDark,
)

// ═══════════════════════════════════════════════════════════════
//  Theme الرئيسي
// ═══════════════════════════════════════════════════════════════

@Composable
fun STTITENIPTVTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    // ─── ضبط Status Bar / Navigation Bar ───
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                window.statusBarColor = TvBackground.toArgb()
                window.navigationBarColor = TvBackground.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            } catch (_: Exception) { }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
