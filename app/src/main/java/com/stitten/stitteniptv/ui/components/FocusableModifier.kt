package com.stitten.stitteniptv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.unit.dp

/**
 * Modifier موحّد للعناصر القابلة للتركيز في Android TV.
 *
 * ✅ يعمل بضغطة واحدة من زر OK/Center في الريموت
 * ✅ تأثير Focus واضح (تكبير + حدود زرقاء + خلفية)
 * ✅ يمنع النقر المزدوج عبر debounce 300ms
 *
 * ⚠️ ملاحظة: `shape` يجب أن يكون قبل `onClick` حتى يعمل trailing lambda
 *    بالشكل: `Modifier.tvFocusable { onClick() }`
 */
fun Modifier.tvFocusable(
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    onClick: () -> Unit,
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        label = "scale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color(0xFF1F6FEB) else Color.Transparent,
        label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) Color(0xFF21262D) else Color.Transparent,
        label = "bg"
    )

    val interaction = remember { MutableInteractionSource() }

    /** نقرة موحّدة مع حماية من التكرار */
    fun safeClick() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 300L) {
            lastClickTime = now
            onClick()
        }
    }

    this
        .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            shape = shape,
            clip = false
        )
        .background(color = bgColor, shape = shape)
        .border(width = 3.dp, color = borderColor, shape = shape)
        .onFocusChanged { isFocused = it.isFocused }

        // ✅ استقبال أزرار الريموت مباشرة — الحل الأساسي للنقرة المزدوجة
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

        // ✅ clickable يبقى للمس (للدعم على الأجهزة اللمسية)
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = { safeClick() }
        )
}
