package com.stitten.stitteniptv.ui.screens

import com.stitten.stitteniptv.data.ChannelRepository
import com.stitten.stitteniptv.data.ErrorLogger
import com.stitten.stitteniptv.data.FavoritesSyncManager
import com.stitten.stitteniptv.data.ParentalControlManager
import com.stitten.stitteniptv.data.PlayerSettingsManager
import com.stitten.stitteniptv.data.SourceManager
import com.stitten.stitteniptv.data.WatchHistoryManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlayerEntryPoint {

    fun watchHistoryManager(): WatchHistoryManager
    fun sourceManager(): SourceManager
    fun errorLogger(): ErrorLogger
    fun parentalControlManager(): ParentalControlManager
    fun favoritesSyncManager(): FavoritesSyncManager
    fun channelRepository(): ChannelRepository

    // ✅ جديد: لاختيار ExoPlayer / VLC
    fun playerSettingsManager(): PlayerSettingsManager
}
