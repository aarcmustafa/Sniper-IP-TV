package com.stitten.stitteniptv.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "series",
    indices = [Index("category"), Index("name")]
)
data class SeriesEntity(
    @PrimaryKey val id: String,
    val name: String,
    val poster: String,
    val category: String,
    val sourceId: Long = 0
)
