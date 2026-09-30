package com.stitten.stitteniptv.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movies",
    indices = [Index("category"), Index("name")]
)
data class MovieEntity(
    @PrimaryKey val id: String,
    val name: String,
    val poster: String,
    val url: String,
    val category: String,
    val sourceId: Long = 0
)
