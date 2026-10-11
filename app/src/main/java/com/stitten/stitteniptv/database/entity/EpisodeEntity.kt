package com.stitten.stitteniptv.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    indices = [
        Index("seriesId"),
        Index("season"),
        Index("episode"),
    ]
)
data class EpisodeEntity(
    @PrimaryKey val id: String,

    /** معرّف المسلسل الذي تنتمي إليه الحلقة */
    val seriesId: String,

    val title: String,
    val season: Int,
    val episode: Int,
    val url: String,
    val poster: String = "",

    // حقول اختيارية
    val plot: String? = null,
    val duration: String? = null,
    val rating: String? = null,
    val containerExtension: String? = null,
    val episodeId: Int? = null,

    /** تاريخ الإضافة (Unix ms) */
    val addedAt: Long = 0L,
)
