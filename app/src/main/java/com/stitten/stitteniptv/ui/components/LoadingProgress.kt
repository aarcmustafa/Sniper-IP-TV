package com.stitten.stitteniptv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * دائرة تحميل موحّدة تدعم وضعين:
 *
 *  1️⃣ Determinate — عند تمرير [progress] بين 0f و 1f
 *      → تعرض دائرة نسبية مع النسبة المئوية
 *
 *  2️⃣ Indeterminate — عند تمرير [progress] = null
 *      → تعرض دائرة دوّارة بدون نسبة
 *
 * @param progress  القيمة بين 0f و 1f أو null للدائرة الدوّارة
 * @param label     نص يظهر أسفل الدائرة (اختياري)
 * @param size      قطر الدائرة
 * @param strokeWidth سماكة الخط
 * @param color     لون الدائرة الأمامية
 * @param trackColor لون الخلفية
 * @param modifier  معدِّل Compose
 */
@Composable
fun LoadingProgress(
    progress: Float?,
    label: String? = null,
    size: Dp = 140.dp,
    strokeWidth: Dp = 10.dp,
    color: Color = Color(0xFF1F6FEB),
    trackColor: Color = Color(0xFF21262D),
    modifier: Modifier = Modifier,
) {
    val isIndeterminate = progress == null

    // ═══════════════════════════════════════════════════════════
    //  الوضع 1: Determinate (نسبة %)
    // ═══════════════════════════════════════════════════════════
    val animatedProgress by animateFloatAsState(
        targetValue = (progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "loadingProgress",
    )

    // ═══════════════════════════════════════════════════════════
    //  الوضع 2: Indeterminate (دائرة دوّارة)
    // ═══════════════════════════════════════════════════════════
    var spinAngle by remember { mutableFloatStateOf(0f) }

    if (isIndeterminate) {
        LaunchedEffect(Unit) {
            var lastFrame = 0L
            while (true) {
                withFrameMillis { now ->
                    if (lastFrame != 0L) {
                        val delta = now - lastFrame
                        spinAngle = (spinAngle + delta * 0.36f) % 360f
                    }
                    lastFrame = now
                }
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val strokePx = strokeWidth.toPx()
                val radius = (this.size.minDimension - strokePx) / 2f
                val topLeft = Offset(
                    (this.size.width - radius * 2) / 2f,
                    (this.size.height - radius * 2) / 2f,
                )
                val arcSize = Size(radius * 2, radius * 2)

                // ═══ الحلقة الخلفية (ثابتة في الوضعين) ═══
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    topLeft = topLeft,
                    size = arcSize,
                )

                if (isIndeterminate) {
                    // ═══ الوضع 2: قوس دوّار (90 درجة فقط) ═══
                    drawArc(
                        color = color,
                        startAngle = spinAngle - 90f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        topLeft = topLeft,
                        size = arcSize,
                    )
                } else {
                    // ═══ الوضع 1: قوس بنسبة progress ═══
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        topLeft = topLeft,
                        size = arcSize,
                    )
                }
            }

            // ═══ النسبة المئوية (فقط في الوضع Determinate) ═══
            if (!isIndeterminate) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (!label.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = label,
                color = Color(0xFF8B949E),
                fontSize = 14.sp,
            )
        }
    }
}
