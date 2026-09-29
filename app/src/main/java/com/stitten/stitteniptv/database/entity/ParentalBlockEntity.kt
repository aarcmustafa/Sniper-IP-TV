package com.stitten.stitteniptv.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parental_blocks")
data class ParentalBlockEntity(
    @PrimaryKey val blockId: String,
    val blockType: String,
    val label: String
)
