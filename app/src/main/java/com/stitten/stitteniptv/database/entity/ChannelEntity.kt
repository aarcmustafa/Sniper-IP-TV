package com.stitten.stitteniptv.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "channels",
    indices = [Index("groupName"), Index("name"), Index("groupPriority")]
)
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logo: String,
    val url: String,
    val groupName: String,
    val groupPriority: Int = 4,
    val sourceId: Long = 0
)
