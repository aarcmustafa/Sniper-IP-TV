package com.stitten.stitteniptv.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val contentId: String,
    val contentType: String,
    val title: String,
    val poster: String,
    val url: String,
    val positionMs: Long,
    val durationMs: Long,
    val season: Int = 0,
    val episode: Int = 0,
    val seriesId: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
