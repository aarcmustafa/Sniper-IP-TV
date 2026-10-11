package com.stitten.stitteniptv.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════════
//  ألوان التطبيق الداكنة (TV Style)
// ═══════════════════════════════════════════════════════════════

val BackgroundDark = Color(0xFF0D1117)
val SurfaceDark = Color(0xFF161B22)
val SurfaceElevated = Color(0xFF1E2536)
val SurfaceHighlight = Color(0xFF21262D)

val PrimaryAccent = Color(0xFF1F6FEB)
val SecondaryAccent = Color(0xFF8B5CF6)
val FocusBorder = Color(0xFF58A6FF)
val GreenAccent = Color(0xFF238636)
val RedAccent = Color(0xFFDA3633)

val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF8B949E)
val TextMuted = Color(0xFF6E7681)
val Divider = Color(0xFF30363D)

// ═══════════════════════════════════════════════════════════════
//  Color Scheme — Dark دائمًا (TV)
// ═══════════════════════════════════════════════════════════════

private val DarkColorScheme = darkColorScheme(
    // ─── الألوان الأساسية ───
    primary = PrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = PrimaryAccent.copy(alpha = 0.2f),
    onPrimaryContainer = TextPrimary,

    // ─── الألوان الثانوية ───
    secondary = SecondaryAccent,
    onSecondary = Color.White,
    secondaryContainer = SecondaryAccent.copy(alpha = 0.2f),
    onSecondaryContainer = TextPrimary,

    // ─── Tertiary ───
    tertiary = GreenAccent,
    onTertiary = Color.White,

    // ─── الخلفيات ───
    background = BackgroundDark,
    onBackground = TextPrimary,

    // ─── الأسطح ───
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHighlight,
    onSurfaceVariant = TextSecondary,

    // ─── الأخطاء ───
    error = RedAccent,
    onError = Color.White,
    errorContainer = RedAccent.copy(alpha = 0.2f),
    onErrorContainer = TextPrimary,

    // ─── الحدود ───
    outline = Divider,
    outlineVariant = Divider.copy(alpha = 0.5f),

    // ─── Inverse ───
    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundDark,
    inversePrimary = PrimaryAccent,
)

// ═══════════════════════════════════════════════════════════════
//  Theme الرئيسي
// ═══════════════════════════════════════════════════════════════

@Composable
fun STTITENIPTVTheme(
    content: @Composable () -> Unit
) {
    // TV دائمًا داكن — لا يوجد وضع فاتح
    val colorScheme = DarkColorScheme

    // ضبط شريط الحالة (Status Bar) في الأجهزة اللوحية
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                window.statusBarColor = BackgroundDark.toArgb()
                window.navigationBarColor = BackgroundDark.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            } catch (_: Exception) { }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,     // ✅ خطوط Cairo
        content = content,
    )
}
