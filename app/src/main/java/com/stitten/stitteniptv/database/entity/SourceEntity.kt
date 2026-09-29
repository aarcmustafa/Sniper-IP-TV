package com.stitten.stitteniptv.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val epgUrl: String = "",
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
