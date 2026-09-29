package com.stitten.stitteniptv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

fun Modifier.tvFocusable(
    onClick: () -> Unit
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.08f else 1f, label = "scale")
    val borderColor by animateColorAsState(
        if (isFocused) Color(0xFF1F6FEB) else Color.Transparent,
        label = "border"
    )
    val bgColor by animateColorAsState(
        if (isFocused) Color(0xFF21262D) else Color.Transparent,
        label = "bg"
    )
    val interaction = remember { MutableInteractionSource() }

    this
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .background(color = bgColor, shape = RoundedCornerShape(12.dp))
        .border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
        .onFocusChanged { isFocused = it.isFocused }
        .focusable(interactionSource = interaction)
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick
        )
}
