package com.stitten.stitteniptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * صف أفقي (نمط Netflix) يعرض قائمة عناصر قابلة للتمرير أفقيًا.
 *
 * @param title  عنوان القسم (اختياري).
 * @param items  قائمة العناصر.
 * @param key    دالة تُعيد مفتاحًا فريدًا لكل عنصر (لتحسين الأداء).
 * @param card   دالة Composable لرسم كل بطاقة (استخدم [PosterCard] مثلًا).
 */
@Composable
fun <T> ContentRow(
    title: String?,
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    card: @Composable (T) -> Unit
) {
    if (items.isEmpty()) return

    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxWidth()) {

        // ===== عنوان القسم =====
        if (!title.isNullOrBlank()) {
            SectionHeader(title = title, count = items.size)
        }

        Spacer(Modifier.height(8.dp))

        // ===== الصف الأفقي =====
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
 * عنوان قسم موحّد لكل الصفوف.
 */
@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "($count)",
                color = Color(0xFF8B949E),
                fontSize = 14.sp,
            )
        }
    }
}
