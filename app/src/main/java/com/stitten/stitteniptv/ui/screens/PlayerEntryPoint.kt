package com.stitten.stitteniptv.ui.screens

import com.stitten.stitteniptv.data.PlayerSettingsManager
import com.stitten.stitteniptv.data.WatchHistoryManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlayerEntryPoint {

    fun watchHistoryManager(): WatchHistoryManager

    // ✅ مُضاف حديثًا لدعم اختيار المحرك (ExoPlayer / VLC / AUTO)
    fun playerSettingsManager(): PlayerSettingsManager
}
