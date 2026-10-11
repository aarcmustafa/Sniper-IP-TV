package com.stitten.stitteniptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * بانر العنصر المميز في أعلى الشاشة الرئيسية.
 */
@Composable
fun HeroBanner(
    title: String,
    posterUrl: String?,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
    backdropUrl: String? = null,
    subtitle: String? = null,
    plot: String? = null,
    rating: String? = null,
    year: String? = null,
    primaryLabel: String = "▶  تشغيل",
    secondaryLabel: String = "ℹ  التفاصيل",
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        // ── الصورة الخلفية ──
        AsyncImage(
            model = backdropUrl ?: posterUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // ── تدرج داكن ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.55f),
                            Color(0xFF0D1117).copy(alpha = 0.98f),
                        )
                    )
                )
        )

        // ── المحتوى ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.55f)
                .padding(start = 40.dp, bottom = 32.dp, end = 16.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            val infoLine = buildList {
                if (!year.isNullOrBlank()) add(year)
                if (!rating.isNullOrBlank()) add("⭐ $rating")
                if (!subtitle.isNullOrBlank()) add(subtitle)
            }.joinToString("  ·  ")

            if (infoLine.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = infoLine,
                    color = Color(0xFFD0D7DE),
                    fontSize = 15.sp,
                )
            }

            if (!plot.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = plot,
                    color = Color(0xFFA8B3C1),
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroPrimaryButton(label = primaryLabel, onClick = onPlay)
                HeroSecondaryButton(label = secondaryLabel, onClick = onDetails)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  الأزرار (داخلية)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HeroPrimaryButton(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .tvFocusable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1F6FEB))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun HeroSecondaryButton(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .tvFocusable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF30363D))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    )
}
