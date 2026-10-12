package com.stitten.stitteniptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary
import com.stitten.stitteniptv.ui.theme.TvTextTertiary

/**
 * صف أفقي (نمط Netflix).
 *
 * @param title        عنوان القسم (اختياري)
 * @param subtitle     وصف فرعي اختياري
 * @param items        عناصر الصف
 * @param key          مفتاح فريد لكل عنصر
 * @param icon         أيقونة القسم (اختياري)
 * @param onSeeAll     callback لزر "عرض الكل" (اختياري — إن لم يُمرر لا يظهر الزر)
 * @param cardSpacing  المسافة بين البطاقات (افتراضي 12dp)
 * @param contentPadding الحافة الأفقية للقائمة
 */
@Composable
fun <T> ContentRow(
    title: String?,
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null,
    cardSpacing: androidx.compose.ui.unit.Dp = Spacing.md,
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.xxl),
    card: @Composable (T) -> Unit
) {
    if (items.isEmpty()) return

    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxWidth()) {

        if (!title.isNullOrBlank()) {
            SectionHeader(
                title = title,
                subtitle = subtitle,
                icon = icon,
                count = items.size,
                onSeeAll = onSeeAll,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(cardSpacing),
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = items,
                key = { key(it) }
            ) { item ->
                card(item)
            }
        }
    }
}

/**
 * عنوان قسم موحّد احترافي:
 *   [🎬]  عنوان القسم  · 44 عنصر             عرض الكل →
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    count: Int? = null,
    onSeeAll: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ═══════════════════════════════════════════════════════
        //  الأيقونة
        // ═══════════════════════════════════════════════════════
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TvPrimary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(Spacing.md))
        }

        // ═══════════════════════════════════════════════════════
        //  العنوان + العدد + الوصف الفرعي
        // ═══════════════════════════════════════════════════════
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = TvText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (count != null && count > 0) {
                    Spacer(Modifier.width(Spacing.sm))
                    CountChip(count = count)
                }
            }

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = TvTextTertiary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // ═══════════════════════════════════════════════════════
        //  زر "عرض الكل"
        // ═══════════════════════════════════════════════════════
        if (onSeeAll != null) {
            Spacer(Modifier.width(Spacing.md))
            SeeAllButton(onClick = onSeeAll)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  مكونات داخلية
// ═══════════════════════════════════════════════════════════════

/**
 * شريحة صغيرة تعرض العدد (مثل: 44 عنصر).
 */
@Composable
private fun CountChip(count: Int) {
    val label = when {
        count == 1 -> "عنصر واحد"
        count == 2 -> "عنصران"
        count in 3..10 -> "$count عناصر"
        else -> "$count عنصرًا"
    }

    Text(
        text = label,
        color = TvTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(TvSurfaceHigh)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/**
 * زر "عرض الكل →".
 */
@Composable
private fun SeeAllButton(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .tvFocusable(onClick = onClick)
            .clip(RoundedCornerShape(Radius.sm))
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    ) {
        Text(
            text = "عرض الكل",
            color = TvPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            // في RTL هذا الاتجاه هو الصحيح "أمام" (لليسار)
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = TvPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}
