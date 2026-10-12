package com.stitten.stitteniptv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.TvFocus
import com.stitten.stitteniptv.ui.theme.TvFocusGlow
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh

// ═══════════════════════════════════════════════════════════════
//  أنماط التركيز الجاهزة
// ═══════════════════════════════════════════════════════════════

/**
 * أنماط تركيز جاهزة — اختر حسب نوع العنصر.
 */
object FocusStyle {
    /** بطاقة كبيرة (فيلم، مسلسل) — تكبير واضح */
    val Card = FocusConfig(
        scaleFocused = 1.06f,
        borderWidth = 3.dp,
        shape = RoundedCornerShape(Radius.md),
        showGlow = true,
    )

    /** قناة مباشرة — تكبير معتدل */
    val Channel = FocusConfig(
        scaleFocused = 1.05f,
        borderWidth = 3.dp,
        shape = RoundedCornerShape(Radius.md),
        showGlow = true,
    )

    /** زر Action — تكبير خفيف */
    val Button = FocusConfig(
        scaleFocused = 1.04f,
        borderWidth = 2.dp,
        shape = RoundedCornerShape(Radius.sm),
        showGlow = false,
    )

    /** Chip تصنيف — تكبير طفيف */
    val Chip = FocusConfig(
        scaleFocused = 1.05f,
        borderWidth = 2.dp,
        shape = RoundedCornerShape(Radius.pill),
        showGlow = false,
    )

    /** عنصر تنقل جانبي */
    val NavItem = FocusConfig(
        scaleFocused = 1.08f,
        borderWidth = 2.dp,
        shape = RoundedCornerShape(Radius.md),
        showGlow = true,
    )
}

/**
 * إعدادات التركيز.
 */
data class FocusConfig(
    val scaleFocused: Float = 1.06f,
    val borderWidth: Dp = 3.dp,
    val shape: RoundedCornerShape = RoundedCornerShape(Radius.md),
    val showGlow: Boolean = true,
    val borderColor: Color = TvFocus,
    val glowColor: Color = TvFocusGlow,
    val backgroundFocused: Color = Color.Transparent,
)

// ═══════════════════════════════════════════════════════════════
//  Overload 1: onClick فقط (توافق كامل مع القديم)
// ═══════════════════════════════════════════════════════════════
fun Modifier.tvFocusable(onClick: () -> Unit): Modifier =
    tvFocusable(config = FocusStyle.Card, onClick = onClick)

// ═══════════════════════════════════════════════════════════════
//  Overload 2: shape + onClick (للتوافق مع القديم)
// ═══════════════════════════════════════════════════════════════
fun Modifier.tvFocusable(
    shape: RoundedCornerShape,
    onClick: () -> Unit,
): Modifier = tvFocusable(
    config = FocusStyle.Card.copy(shape = shape),
    onClick = onClick,
)

// ═══════════════════════════════════════════════════════════════
//  Overload 3: نمط جاهز + onClick (الطريقة الاحترافية)
// ═══════════════════════════════════════════════════════════════
fun Modifier.tvFocusable(
    style: FocusConfig,
    onClick: () -> Unit,
): Modifier = tvFocusable(config = style, onClick = onClick)

// ═══════════════════════════════════════════════════════════════
//  التنفيذ الأساسي
// ═══════════════════════════════════════════════════════════════
fun Modifier.tvFocusable(
    config: FocusConfig,
    onClick: () -> Unit,
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    // ─── Animations (spring لحياة أكثر) ───
    val scale by animateFloatAsState(
        targetValue = if (isFocused) config.scaleFocused else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "focus_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) config.borderColor else Color.Transparent,
        label = "focus_border"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused) config.backgroundFocused else Color.Transparent,
        label = "focus_bg"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused && config.showGlow) 0.6f else 0f,
        label = "focus_glow"
    )

    val interaction = remember { MutableInteractionSource() }

    /** نقرة موحّدة مع حماية من التكرار (200ms) */
    fun safeClick() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 200L) {
            lastClickTime = now
            onClick()
        }
    }

    this
        // ═══════════════════════════════════════════════════
        //  التأثيرات الرسومية
        // ═══════════════════════════════════════════════════
        .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            shape = config.shape,
            clip = false,
        )
        // ─── Glow (توهج خارجي) ───
        .drawBehind {
            if (glowAlpha > 0f) {
                val glowWidth = 8.dp.toPx()
                drawRoundRect(
                    color = config.glowColor.copy(alpha = glowAlpha * 0.35f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        config.shape.topStart.toPx(size, this),
                        config.shape.topStart.toPx(size, this)
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = glowWidth
                    )
                )
            }
        }
        // ─── خلفية التركيز ───
        .background(color = backgroundColor, shape = config.shape)
        // ─── حدود التركيز ───
        .border(
            width = config.borderWidth,
            color = borderColor,
            shape = config.shape
        )
        // ─── مراقبة حالة التركيز ───
        .onFocusChanged { isFocused = it.isFocused }

        // ═══════════════════════════════════════════════════
        //  استقبال أزرار الريموت — الحل الأساسي لـ Android TV
        // ═══════════════════════════════════════════════════
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyUp) return@onKeyEvent false

            when (event.key) {
                Key.DirectionCenter,
                Key.Enter,
                Key.NumPadEnter,
                Key.Spacebar -> {
                    safeClick()
                    true
                }
                else -> false
            }
        }
        .focusable(interactionSource = interaction)

        // ═══════════════════════════════════════════════════
        //  clickable للمس (للأجهزة اللمسية)
        // ═══════════════════════════════════════════════════
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = { safeClick() }
        )
}
