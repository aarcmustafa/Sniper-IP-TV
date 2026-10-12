package com.stitten.stitteniptv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvBackground
import com.stitten.stitteniptv.ui.theme.TvGold
import com.stitten.stitteniptv.ui.theme.TvLive
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvSkeleton
import com.stitten.stitteniptv.ui.theme.TvSkeletonHighlight
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary

/**
 * نوع البطاقة — يحدّد النسبة والمقاس.
 */
enum class PosterCardType {
    MOVIE,    // 2:3 — بوستر سينمائي
    SERIES,   // 2:3 — بوستر مسلسل
    CHANNEL,  // 16:9 — شعار قناة
}

/**
 * بطاقة موحّدة احترافية للأفلام والمسلسلات والقنوات.
 *
 * الميزات:
 *   - Shimmer أثناء تحميل الصورة
 *   - شارة LIVE نابضة للقنوات المباشرة
 *   - شارة جودة (HD/4K) اختيارية
 *   - شريط تقدم المشاهدة
 *   - تأثير Focus Scale + Glow
 */
@Composable
fun PosterCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    progress: Float? = null,
    isChannel: Boolean = false,
    isLive: Boolean = false,
    quality: String? = null,       // "HD" / "4K" / "FHD"
    rating: String? = null,         // "8.5"
) {
    val type = when {
        isChannel -> PosterCardType.CHANNEL
        else -> PosterCardType.MOVIE
    }

    val cardWidth: Dp = when (type) {
        PosterCardType.MOVIE, PosterCardType.SERIES -> 180.dp
        PosterCardType.CHANNEL -> 240.dp
    }

    val cardHeight: Dp = when (type) {
        PosterCardType.MOVIE, PosterCardType.SERIES -> 260.dp  // 2:3
        PosterCardType.CHANNEL -> 135.dp                        // 16:9
    }

    val cornerRadius = RoundedCornerShape(Radius.md)

    Column(
        modifier = modifier
            .width(cardWidth)
            .tvFocusable(onClick = onClick),
        horizontalAlignment = Alignment.Start,
    ) {
        // ═══════════════════════════════════════════════════════
        //  الصورة
        // ═══════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clip(cornerRadius)
                .background(TvSurfaceHigh),
        ) {
            // ─── Shimmer + صورة ───
            if (!imageUrl.isNullOrBlank()) {
                ShimmerLoading(
                    isLoading = false,
                    modifier = Modifier.fillMaxSize()
                )

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .memoryCacheKey(imageUrl)
                        .diskCacheKey(imageUrl)
                        .crossfade(300)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .size(600, 600)
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // ─── Placeholder متدرج ───
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(TvSurfaceHigh, TvBackground)
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title.take(1).uppercase(),
                        color = TvTextSecondary.copy(alpha = 0.4f),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            //  طبقة تدرج سفلية (لقراءة النص فوق الصورة)
            // ═══════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // ═══════════════════════════════════════════════════
            //  شارة LIVE (يمين أعلى)
            // ═══════════════════════════════════════════════════
            if (isLive) {
                LiveBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.sm)
                )
            }

            // ═══════════════════════════════════════════════════
            //  شارة الجودة (يسار أعلى)
            // ═══════════════════════════════════════════════════
            if (!quality.isNullOrBlank() && !isLive) {
                QualityBadge(
                    text = quality,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.sm)
                )
            }

            // ═══════════════════════════════════════════════════
            //  شارة التقييم (يسار أعلى) — للمحتوى غير المباشر
            // ═══════════════════════════════════════════════════
            if (!rating.isNullOrBlank() && !isLive && quality.isNullOrBlank()) {
                RatingChip(
                    rating = rating,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.sm)
                )
            }

            // ═══════════════════════════════════════════════════
            //  شريط تقدم المشاهدة (أسفل)
            // ═══════════════════════════════════════════════════
            if (progress != null && progress in 0f..1f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(TvPrimary)
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            //  اسم القناة داخل الصورة (للقنوات فقط)
            // ═══════════════════════════════════════════════════
            if (isChannel && !subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                )
            }
        }

        // ═══════════════════════════════════════════════════════
        //  العنوان أسفل البطاقة (لغير القنوات)
        // ═══════════════════════════════════════════════════════
        if (!isChannel) {
            Spacer(Modifier.height(Spacing.sm))

            Text(
                text = title,
                color = TvText,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = subtitle,
                    color = TvTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  مكونات داخلية
// ═══════════════════════════════════════════════════════════════

/**
 * شارة LIVE نابضة.
 */
@Composable
private fun LiveBadge(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(Radius.xs))
            .background(TvLive.copy(alpha = alpha))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
        Text(
            text = "LIVE",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
        )
    }
}

/**
 * شارة جودة (HD / 4K / FHD).
 */
@Composable
private fun QualityBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TvGold,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.5.sp,
        modifier = modifier
            .clip(RoundedCornerShape(Radius.xs))
            .background(Color.Black.copy(alpha = 0.7f))
            .border(1.dp, TvGold.copy(alpha = 0.6f), RoundedCornerShape(Radius.xs))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/**
 * شارة التقييم ⭐ 8.5
 */
@Composable
private fun RatingChip(rating: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
            .clip(RoundedCornerShape(Radius.xs))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = TvGold,
            modifier = Modifier.size(10.dp)
        )
        Text(
            text = rating,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Shimmer placeholder — يُستخدم أثناء تحميل الصور.
 * بسيط الآن، يمكن استبداله بـ ShimmerBox لاحقًا.
 */
@Composable
private fun ShimmerLoading(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isLoading) return

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translate by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(TvSkeleton, TvSkeletonHighlight, TvSkeleton),
                start = androidx.compose.ui.geometry.Offset(translate, 0f),
                end = androidx.compose.ui.geometry.Offset(translate + 400f, 400f)
            )
        )
    )
}
