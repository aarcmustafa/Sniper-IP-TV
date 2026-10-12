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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvBackground
import com.stitten.stitteniptv.ui.theme.TvSkeleton
import com.stitten.stitteniptv.ui.theme.TvSkeletonHighlight

// ═══════════════════════════════════════════════════════════════
//  Skeleton Box — الوحدة الأساسية
//  Shimmer حقيقي يتحرك أفقيًا عبر العنصر
// ═══════════════════════════════════════════════════════════════

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Radius.md,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1400,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(TvSkeleton)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        TvSkeleton,
                        TvSkeletonHighlight,
                        TvSkeleton,
                    ),
                    start = Offset(translateX, 0f),
                    end = Offset(translateX + 400f, 400f),
                )
            )
    )
}

// ═══════════════════════════════════════════════════════════════
//  Skeleton Poster Card — يطابق PosterCard الحقيقي
// ═══════════════════════════════════════════════════════════════

@Composable
fun SkeletonPosterCard(
    cardWidth: Dp = 180.dp,
    cardHeight: Dp = 260.dp,
    isChannel: Boolean = false,
) {
    Column(modifier = Modifier.width(cardWidth)) {
        // ─── الصورة ───
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight),
            cornerRadius = Radius.md,
        )

        // ─── العنوان ───
        if (!isChannel) {
            Spacer(Modifier.height(Spacing.sm))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(13.dp),
                cornerRadius = Radius.xs,
            )
            Spacer(Modifier.height(Spacing.xs))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(11.dp),
                cornerRadius = Radius.xs,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Skeleton Hero Banner — يطابق HeroBanner الحقيقي
// ═══════════════════════════════════════════════════════════════

@Composable
fun SkeletonHeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl)
            .height(460.dp)
            .clip(RoundedCornerShape(Radius.lg))
    ) {
        // ─── خلفية البانر ───
        SkeletonBox(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = Radius.lg,
        )

        // ─── محتوى وهمي (Badges + Title + Buttons) ───
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.55f)
                .padding(start = 40.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Badges
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SkeletonBox(Modifier.width(40.dp).height(20.dp), Radius.xs)
                SkeletonBox(Modifier.width(50.dp).height(20.dp), Radius.xs)
                SkeletonBox(Modifier.width(60.dp).height(20.dp), Radius.xs)
            }

            Spacer(Modifier.height(Spacing.xs))

            // Title (سطران)
            SkeletonBox(
                modifier = Modifier.fillMaxWidth(0.95f).height(38.dp),
                cornerRadius = Radius.sm,
            )
            SkeletonBox(
                modifier = Modifier.fillMaxWidth(0.65f).height(38.dp),
                cornerRadius = Radius.sm,
            )

            Spacer(Modifier.height(Spacing.xs))

            // Subtitle
            SkeletonBox(
                modifier = Modifier.fillMaxWidth(0.4f).height(16.dp),
                cornerRadius = Radius.xs,
            )

            // Plot (سطران)
            Spacer(Modifier.height(Spacing.sm))
            SkeletonBox(
                modifier = Modifier.fillMaxWidth(0.9f).height(14.dp),
                cornerRadius = Radius.xs,
            )
            SkeletonBox(
                modifier = Modifier.fillMaxWidth(0.7f).height(14.dp),
                cornerRadius = Radius.xs,
            )

            Spacer(Modifier.height(Spacing.md))

            // Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SkeletonBox(Modifier.width(140.dp).height(48.dp), Radius.md)
                SkeletonBox(Modifier.width(140.dp).height(48.dp), Radius.md)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Skeleton Category Chips — شريط تصنيفات وهمي
// ═══════════════════════════════════════════════════════════════

@Composable
fun SkeletonCategoryChips() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(horizontal = Spacing.xxl),
    ) {
        items(listOf(80, 100, 70, 90, 120, 85)) { width ->
            SkeletonBox(
                modifier = Modifier
                    .width(width.dp)
                    .height(36.dp),
                cornerRadius = Radius.pill,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Skeleton Row — صف أفقي كامل
// ═══════════════════════════════════════════════════════════════

@Composable
fun SkeletonRow(
    titleWidth: Dp = 180.dp,
    count: Int = 6,
    cardWidth: Dp = 180.dp,
    cardHeight: Dp = 260.dp,
    isChannel: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // ─── العنوان ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBox(
                modifier = Modifier.width(titleWidth).height(22.dp),
                cornerRadius = Radius.sm,
            )
            Spacer(Modifier.weight(1f))
            SkeletonBox(
                modifier = Modifier.width(80.dp).height(16.dp),
                cornerRadius = Radius.xs,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        // ─── البطاقات ───
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            contentPadding = PaddingValues(horizontal = Spacing.xxl),
        ) {
            items(count = count) {
                SkeletonPosterCard(
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    isChannel = isChannel,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Skeleton Sidebar — الشريط الجانبي
// ═══════════════════════════════════════════════════════════════

@Composable
fun SkeletonSidebar() {
    Column(
        modifier = Modifier
            .width(140.dp)
            .padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ─── الشعار ───
        SkeletonBox(
            modifier = Modifier.size(36.dp),
            cornerRadius = Radius.sm,
        )

        Spacer(Modifier.height(Spacing.sm))

        // ─── أزرار التنقل ───
        repeat(7) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                SkeletonBox(
                    modifier = Modifier.size(34.dp),
                    cornerRadius = Radius.sm,
                )
                SkeletonBox(
                    modifier = Modifier.width(70.dp).height(12.dp),
                    cornerRadius = Radius.xs,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Skeleton Home Screen — الشاشة الرئيسية كاملة
// ═══════════════════════════════════════════════════════════════

@Composable
fun SkeletonHomeScreen() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground),
    ) {
        // ─── الشريط الجانبي ───
        SkeletonSidebar()

        // ─── المحتوى ───
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = Spacing.md),
        ) {
            Spacer(Modifier.height(Spacing.xl))

            // ─── Chips (تصنيفات) ───
            SkeletonCategoryChips()

            Spacer(Modifier.height(Spacing.lg))

            // ─── Hero Banner ───
            SkeletonHeroBanner()

            Spacer(Modifier.height(Spacing.xxl))

            // ─── صف الأفلام ───
            SkeletonRow(
                titleWidth = 160.dp,
                count = 6,
            )

            Spacer(Modifier.height(Spacing.xxl))

            // ─── صف القنوات ───
            SkeletonRow(
                titleWidth = 200.dp,
                count = 6,
                cardWidth = 240.dp,
                cardHeight = 135.dp,
                isChannel = true,
            )

            Spacer(Modifier.height(Spacing.xxl))

            // ─── صف المسلسلات ───
            SkeletonRow(
                titleWidth = 140.dp,
                count = 6,
            )

            Spacer(Modifier.height(Spacing.xxxl))
        }
    }
}
