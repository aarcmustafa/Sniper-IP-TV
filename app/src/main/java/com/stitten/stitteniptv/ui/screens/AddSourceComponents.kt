package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvSurfaceHigh
import com.stitten.stitteniptv.ui.theme.TvText
import com.stitten.stitteniptv.ui.theme.TvTextSecondary

// ═══════════════════════════════════════════════════════════════
//  Tab Button — لتبديل أوضاع M3U / USB / Xtream
// ═══════════════════════════════════════════════════════════════

@Composable
internal fun AddSourceTabButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (selected) Color.White else TvTextSecondary,
        fontSize = 15.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .tvFocusable(onClick = { if (enabled) onClick() })
            .background(
                color = if (selected) TvPrimary else TvSurfaceHigh,
                shape = RoundedCornerShape(Radius.md)
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
    )
}

// ═══════════════════════════════════════════════════════════════
//  Progress Card — بطاقة تحميل أنيقة أسفل النموذج
// ═══════════════════════════════════════════════════════════════

@Composable
internal fun AddSourceProgressCard(
    progress: Int,
    message: String,
) {
    Box(
        modifier = Modifier
            .width(600.dp)
            .background(
                color = Color(0xFF161B22),
                shape = RoundedCornerShape(Radius.lg)
            )
            .padding(Spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                CircularProgressIndicator(
                    color = TvPrimary,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                )
                Text(
                    text = "$progress%",
                    color = TvText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(Spacing.md))

            Text(
                text = message.ifBlank { "جارٍ التحميل..." },
                color = TvTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.md))

            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = TvPrimary,
                trackColor = TvSurfaceHigh,
            )
        }
    }
}
