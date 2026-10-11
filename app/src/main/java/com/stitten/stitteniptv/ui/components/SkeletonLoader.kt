package com.stitten.stitteniptv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )

    val baseColor = Color(0xFF161B22)
    val highlightColor = Color(0xFF21262D)
    val blended = lerpColor(baseColor, highlightColor, 0.4f + 0.4f * shimmer)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        baseColor,
                        blended,
                        baseColor,
                    )
                )
            )
    )
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f,
    )
}

@Composable
fun SkeletonPosterCard(
    cardWidth: Dp = 200.dp,
    cardHeight: Dp = 300.dp,
) {
    Column(modifier = Modifier.width(cardWidth)) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight),
            cornerRadius = 12.dp,
        )
        Spacer(Modifier.height(10.dp))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(14.dp),
            cornerRadius = 4.dp,
        )
        Spacer(Modifier.height(6.dp))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp),
            cornerRadius = 4.dp,
        )
    }
}

@Composable
fun SkeletonRow(
    title: String? = null,
    count: Int = 6,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            SkeletonBox(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .width(180.dp)
                    .height(20.dp),
                cornerRadius = 4.dp,
            )
            Spacer(Modifier.height(12.dp))
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            items(count = count) {
                SkeletonPosterCard()
            }
        }
    }
}

@Composable
fun SkeletonHomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBox(
                modifier = Modifier.width(180.dp).height(22.dp),
                cornerRadius = 6.dp,
            )
            Spacer(Modifier.weight(1f))
            SkeletonBox(
                modifier = Modifier.width(220.dp).height(24.dp),
                cornerRadius = 6.dp,
            )
        }

        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(380.dp),
            cornerRadius = 16.dp,
        )

        Spacer(Modifier.height(28.dp))

        SkeletonRow(title = "أحدث الأفلام")
        Spacer(Modifier.height(28.dp))
        SkeletonRow(title = "المسلسلات")
        Spacer(Modifier.height(28.dp))
        SkeletonRow(title = "قنوات مباشرة")
    }
}
