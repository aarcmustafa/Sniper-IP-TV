package com.stitten.stitteniptv.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stitten.stitteniptv.ui.theme.PrimaryAccent

/**
 * صف أفقي (نمط Netflix).
 *
 * @param icon  أيقونة المادة اختيارية للعنوان (بدل الإيموجي).
 */
@Composable
fun <T> ContentRow(
    title: String?,
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    card: @Composable (T) -> Unit
) {
    if (items.isEmpty()) return

    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxWidth()) {

        if (!title.isNullOrBlank()) {
            SectionHeader(
                title = title,
                icon = icon,
                count = items.size,
            )
        }

        Spacer(Modifier.height(12.dp))

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
 * عنوان قسم موحّد.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    count: Int? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ─── أيقونة ───
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryAccent,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(10.dp))
        }

        // ─── العنوان ───
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        // ─── العدد ───
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
