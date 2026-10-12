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

// ═══════════════════════════════════════════════════════════════
//  أنماط التركيز الجاهزة
// ═══════════════════════════════════════════════════════════════

object FocusStyle {
    val Card = FocusConfig(
        scaleFocused = 1.06f,
        borderWidth = 3.dp,
        shape = RoundedCornerShape(Radius.md),
        showGlow = true,
    )

    val Channel = FocusConfig(
        scaleFocused = 1.05f,
        borderWidth = 3.dp,
        shape = RoundedCornerShape(Radius.md),
        showGlow = true,
    )

    val Button = FocusConfig(
        scaleFocused = 1.04f,
        borderWidth = 2.dp,
        shape = RoundedCornerShape(Radius.sm),
        showGlow = false,
    )

    val Chip = FocusConfig(
        scaleFocused = 1.05f,
        borderWidth = 2.dp,
        shape = RoundedCornerShape(Radius.pill),
        showGlow = false,
    )

    val NavItem = FocusConfig(
        scaleFocused = 1.08f,
        borderWidth = 2.dp,
        shape = RoundedCornerShape(Radius.md),
        showGlow = true,
    )
}

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
//  Overload 1: onClick فقط
// ═══════════════════════════════════════════════════════════════
fun Modifier.tvFocusable(onClick: () -> Unit): Modifier =
    tvFocusable(style = FocusStyle.Card, onClick = onClick)

// ═══════════════════════════════════════════════════════════════
//  Overload 2: shape + onClick (توافق مع القديم)
// ═══════════════════════════════════════════════════════════════
fun Modifier.tvFocusable(
    shape: RoundedCornerShape,
    onClick: () -> Unit,
): Modifier = tvFocusable(
    style = FocusStyle.Card.copy(shape = shape),
    onClick = onClick,
)

// ═══════════════════════════════════════════════════════════════
//  Overload 3: نمط جاهز + onClick (الطريقة الأساسية)
// ═══════════════════════════════════════════════════════════════
fun Modifier.tvFocusable(
    style: FocusConfig,
    onClick: () -> Unit,
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) style.scaleFocused else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "focus_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) style.borderColor else Color.Transparent,
        label = "focus_border"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused) style.backgroundFocused else Color.Transparent,
        label = "focus_bg"
    )

    val interaction = remember { MutableInteractionSource() }

    fun safeClick() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 200L) {
            lastClickTime = now
            onClick()
        }
    }

    this
        .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            shape = style.shape,
            clip = false,
        )
        .background(color = backgroundColor, shape = style.shape)
        .border(
            width = style.borderWidth,
            color = borderColor,
            shape = style.shape
        )
        .onFocusChanged { isFocused = it.isFocused }
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
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = { safeClick() }
        )
}
