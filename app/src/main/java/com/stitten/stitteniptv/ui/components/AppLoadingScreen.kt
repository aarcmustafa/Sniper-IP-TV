package com.stitten.stitteniptv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvBackground
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary

/**
 * شاشة تحميل كاملة احترافية.
 * تُعرض أثناء العمليات الطويلة (>5 ثوان):
 *   - تحميل قوائم M3U من السيرفر
 *   - مزامنة Xtream الكاملة
 *   - تسجيل الدخول الأول
 *
 * @param progress  0f..1f لعرض نسبة مئوية، أو null لشريط لا نهائي
 * @param message   نص الحالة (مثل: "جارٍ تحميل القنوات...")
 * @param subtitle  نص فرعي اختياري
 */
@Composable
fun AppLoadingScreen(
    progress: Float? = null,
    message: String = "جارٍ التحميل...",
    subtitle: String? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        TvBackground,
                        Color(0xFF0D1014),
                        TvBackground,
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(Spacing.xxxl),
        ) {
            // ═══════════════════════════════════════════════════
            //  شعار متحرك
            // ═══════════════════════════════════════════════════
            AnimatedLogo()

            Spacer(Modifier.height(Spacing.xxxl))

            // ═══════════════════════════════════════════════════
            //  اسم التطبيق
            // ═══════════════════════════════════════════════════
            Text(
                text = "STTITEN IP TV",
                color = TvText,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
            )

            Spacer(Modifier.height(Spacing.sm))

            // ═══════════════════════════════════════════════════
            //  رسالة الحالة
            // ═══════════════════════════════════════════════════
            Text(
                text = message,
                color = TvTextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )

            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = subtitle,
                    color = TvTextSecondary.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(Spacing.xxl))

            // ═══════════════════════════════════════════════════
            //  شريط التقدم
            // ═══════════════════════════════════════════════════
            if (progress != null) {
                DeterminateProgressBar(
                    progress = progress.coerceIn(0f, 1f),
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "${(progress * 100).toInt().coerceIn(0, 100)}%",
                    color = TvPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                IndeterminateProgressBar()
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  شعار متحرك
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AnimatedLogo() {
    val transition = rememberInfiniteTransition(label = "logo_pulse")

    val scale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logo_scale",
    )

    val glowAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logo_glow",
    )

    Box(contentAlignment = Alignment.Center) {
        // ─── هالة خلفية ───
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TvPrimary.copy(alpha = glowAlpha * 0.4f),
                            Color.Transparent,
                        )
                    )
                )
        )

        // ─── أيقونة الشعار ───
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(scale)
                .clip(RoundedCornerShape(Radius.xl))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            TvPrimary,
                            Color(0xFF2563EB),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "🎬",
                fontSize = 44.sp,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  شريط تقدم محدد (Determinate)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DeterminateProgressBar(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "progress",
    )

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(Radius.pill))
            .background(TvSurfaceHigh),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(6.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            TvPrimary,
                            Color(0xFF60A5FA),
                        )
                    )
                )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  شريط تقدم لا نهائي (Indeterminate)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun IndeterminateProgressBar() {
    val transition = rememberInfiniteTransition(label = "indeterminate")

    val translateX by transition.animateFloat(
        initialValue = -100f,
        targetValue = 380f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "indeterminate_x",
    )

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(Radius.pill))
            .background(TvSurfaceHigh),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.35f)
                .height(6.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TvPrimary,
                            Color(0xFF60A5FA),
                            Color.Transparent,
                        )
                    )
                )
                .graphicsLayer(translationX = translateX)
        )
    }
}
