package com.stitten.stitteniptv.ui.screens

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
    fun errorLogger(): ErrorLogger
    fun playerSettingsManager(): PlayerSettingsManager
    fun parentalControlManager(): ParentalControlManager
    fun sourceManager(): SourceManager
    fun favoritesSyncManager(): FavoritesSyncManager
}
