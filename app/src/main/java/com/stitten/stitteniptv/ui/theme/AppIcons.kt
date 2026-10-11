package com.stitten.stitteniptv.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * أيقونات التطبيق المركزية.
 * استبدال الإيموجي بـ Material Icons للاحترافية.
 */
object AppIcons {

    // ═══════════════════════════════════════════════════════════
    //  التصنيفات الرئيسية
    // ═══════════════════════════════════════════════════════════

    val Movies: ImageVector = Icons.Filled.Movie
    val Series: ImageVector = Icons.Filled.Tv
    val LiveTv: ImageVector = Icons.Filled.LiveTv
    val Favorites: ImageVector = Icons.Filled.Favorite
    val Search: ImageVector = Icons.Filled.Search
    val Settings: ImageVector = Icons.Filled.Settings
    val Home: ImageVector = Icons.Filled.Home

    // ═══════════════════════════════════════════════════════════
    //  الإجراءات
    // ═══════════════════════════════════════════════════════════

    val Refresh: ImageVector = Icons.Filled.Refresh
    val Play: ImageVector = Icons.Filled.PlayArrow
    val Close: ImageVector = Icons.Filled.Close
    val ArrowBack: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val ArrowForward: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight

    // ═══════════════════════════════════════════════════════════
    //  المؤشرات
    // ═══════════════════════════════════════════════════════════

    val Rating: ImageVector = Icons.Filled.Star
    val Trending: ImageVector = Icons.Filled.TrendingUp
    val Check: ImageVector = Icons.Filled.CheckCircle
    val Info: ImageVector = Icons.Filled.Info

    // ═══════════════════════════════════════════════════════════
    //  المصادر والإعدادات
    // ═══════════════════════════════════════════════════════════

    val Source: ImageVector = Icons.Filled.Link
    val Folder: ImageVector = Icons.Filled.FolderOpen
    val Lock: ImageVector = Icons.Filled.Lock
    val Person: ImageVector = Icons.Filled.Person
    val Storage: ImageVector = Icons.Filled.Storage
    val Wifi: ImageVector = Icons.Filled.Wifi
}
