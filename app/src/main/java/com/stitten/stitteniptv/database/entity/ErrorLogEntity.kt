package com.stitten.stitteniptv.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "error_logs")
data class ErrorLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val errorMessage: String,
    val errorCode: Int,
    val timestamp: Long = System.currentTimeMillis()
)
