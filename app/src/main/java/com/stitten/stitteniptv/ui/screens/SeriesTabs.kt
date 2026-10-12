package com.stitten.stitteniptv.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stitten.stitteniptv.data.Episode
import com.stitten.stitteniptv.database.entity.SeriesEntity
import com.stitten.stitteniptv.ui.components.FocusStyle
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.theme.AppIcons
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvBackground
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvSurface
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary
import com.stitten.stitteniptv.ui.theme.TvTextTertiary

// ═══════════════════════════════════════════════════════════════
//  تبويب: نظرة عامة
// ═══════════════════════════════════════════════════════════════

@Composable
fun SeriesOverviewTab(
    series: SeriesEntity?,
    episodeCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxxl, vertical = Spacing.sm)
    ) {
        if (!series?.plot.isNullOrBlank()) {
            SectionTitle(text = "القصة")
            Text(
                text = series!!.plot!!,
                color = TvTextSecondary,
                fontSize = 15.sp,
                lineHeight = 26.sp,
            )
            Spacer(Modifier.height(Spacing.xxl))
        }

        val details = buildList {
            if (!series?.director.isNullOrBlank()) add("المخرج" to series!!.director!!)
            if (!series?.genre.isNullOrBlank()) add("التصنيف" to series!!.genre!!)
            if (!series?.year.isNullOrBlank()) add("السنة" to series!!.year!!)
            if (!series?.category.isNullOrBlank()) add("الفئة" to series!!.category)
            if (episodeCount > 0) add("عدد الحلقات" to "$episodeCount")
            if (series != null && series.seasonCount > 0) {
                add("عدد المواسم" to "${series.seasonCount}")
            }
        }

        if (details.isNotEmpty()) {
            SectionTitle(text = "معلومات")
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                details.forEach { (label, value) ->
                    DetailInfoRow(label = label, value = value)
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = label,
            color = TvTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(100.dp),
        )
        Text(
            text = value,
            color = TvText,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  تبويب: الفريق
// ═══════════════════════════════════════════════════════════════

@Composable
fun SeriesCastTab(series: SeriesEntity?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxxl, vertical = Spacing.sm)
    ) {
        if (series?.cast.isNullOrBlank() && series?.director.isNullOrBlank()) {
            EmptyTabState(message = "لا توجد معلومات عن الفريق")
            return@Column
        }

        if (!series?.director.isNullOrBlank()) {
            SectionTitle(text = "المخرج")
            PersonChip(name = series!!.director!!, role = "مخرج")
            Spacer(Modifier.height(Spacing.xl))
        }

        if (!series?.cast.isNullOrBlank()) {
            SectionTitle(text = "الممثلون")
            val actors = series!!.cast!!
                .split(",", "،")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                actors.forEach { actor ->
                    PersonChip(name = actor, role = "ممثل")
                }
            }
        }
    }
}

@Composable
private fun PersonChip(name: String, role: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(TvSurfaceHigh)
            .padding(
                horizontal = Spacing.lg,
                vertical = Spacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            SeriesAccent.copy(alpha = 0.3f),
                            SeriesAccent.copy(alpha = 0.1f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = SeriesAccent,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Spacer(Modifier.width(Spacing.md))

        Column {
            Text(
                text = name,
                color = TvText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = role,
                color = TvTextSecondary,
                fontSize = 12.sp,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Episode Row — حلقة واحدة
// ═══════════════════════════════════════════════════════════════

@Composable
fun EpisodeRow(
    episode: Episode,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = Spacing.xxxl,
                vertical = Spacing.sm,
            )
            .clip(RoundedCornerShape(Radius.md))
            .background(TvSurface)
            .tvFocusable(
                style = FocusStyle.Card,
                onClick = onClick,
            )
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        // ─── رقم الحلقة ───
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            SeriesAccent,
                            Color(0xFF6D28D9),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${episode.number}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        // ─── معلومات الحلقة ───
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = episode.title,
                color = TvText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = "الموسم ${episode.season}",
                    color = TvTextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )

                if (!episode.duration.isNullOrBlank()) {
                    Text(
                        text = "•",
                        color = TvTextTertiary,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = episode.duration!!,
                        color = TvTextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // ─── زر التشغيل ───
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SeriesAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "تشغيل",
                tint = SeriesAccent,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  مكونات مساعدة
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = TvText,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = Spacing.md),
    )
}

@Composable
private fun EmptyTabState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = TvTextSecondary,
            fontSize = 15.sp,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  Skeleton & Empty
// ═══════════════════════════════════════════════════════════════

@Composable
fun SeriesDetailsSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            TvSurfaceHigh,
                            TvBackground,
                        )
                    )
                )
        )

        Spacer(Modifier.height(Spacing.xxl))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xxxl),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(TvSurfaceHigh)
                )
            }
        }
    }
}

@Composable
fun EmptySeriesView(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(TvSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Series,
                    contentDescription = null,
                    tint = TvTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp),
                )
            }

            Text(
                text = "لا توجد حلقات",
                color = TvText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "لم نتمكن من العثور على حلقات هذا المسلسل",
                color = TvTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.sm))

            Text(
                text = "← رجوع",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .tvFocusable(
                        style = FocusStyle.Button,
                        onClick = onBack,
                    )
                    .clip(RoundedCornerShape(Radius.md))
                    .background(TvPrimary)
                    .padding(
                        horizontal = Spacing.xxl,
                        vertical = Spacing.md,
                    ),
            )
        }
    }
}
