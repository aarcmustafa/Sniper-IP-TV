package com.stitten.stitteniptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stitten.stitteniptv.ui.components.FocusStyle
import com.stitten.stitteniptv.ui.components.tvFocusable
import com.stitten.stitteniptv.ui.theme.AppIcons
import com.stitten.stitteniptv.ui.theme.Radius
import com.stitten.stitteniptv.ui.theme.Spacing
import com.stitten.stitteniptv.ui.theme.TvLive
import com.stitten.stitteniptv.ui.theme.TvPrimary
import com.stitten.stitteniptv.ui.theme.TvSuccess
import com.stitten.stitteniptv.ui.theme.TvSurface
import com.stitten.stitteniptv.ui.theme.TvText

/**
 * الشريط الجانبي الرئيسي — قابل لإعادة الاستخدام في كل الشاشات.
 */
@Composable
fun DashboardSidebar(
    onMoviesClick: () -> Unit,
    onSeriesListClick: () -> Unit,
    onLiveClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onSearchClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(130.dp)
            .background(TvSurface)
            .padding(vertical = Spacing.xl, horizontal = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        // ═══════ الشعار ═══════
        SidebarLogo()

        Spacer(Modifier.height(Spacing.xl))

        // ═══════ عناصر التنقل الرئيسية ═══════
        SideNavButton(
            icon = Icons.Default.Movie,
            label = "أفلام",
            onClick = onMoviesClick,
        )
        SideNavButton(
            icon = Icons.Default.Tv,
            label = "مسلسلات",
            onClick = onSeriesListClick,
        )
        SideNavButton(
            icon = Icons.Default.LiveTv,
            label = "مباشر",
            tint = TvLive,
            onClick = onLiveClick,
        )
        SideNavButton(
            icon = Icons.Default.Favorite,
            label = "المفضلة",
            tint = Color(0xFFF85149),
            onClick = onFavoritesClick,
        )
        SideNavButton(
            icon = Icons.Default.Search,
            label = "بحث",
            onClick = onSearchClick,
        )

        // ═══════ دفع الأزرار السفلية ═══════
        Spacer(Modifier.weight(1f))

        // ═══════ عناصر سفلية ═══════
        SideNavButton(
            icon = Icons.Default.Refresh,
            label = "مزامنة",
            tint = TvSuccess,
            onClick = onRefreshClick,
        )
        SideNavButton(
            icon = Icons.Default.Settings,
            label = "إعدادات",
            onClick = onSettingsClick,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  الشعار
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SidebarLogo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(
                    Brush.linearGradient(
                        colors = listOf(TvPrimary, Color(0xFF2563EB))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Movies,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "STTITEN",
            color = TvPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  زر التنقل
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SideNavButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = TvText,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(
                style = FocusStyle.NavItem,
                onClick = onClick,
            )
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = label,
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
