package com.stitten.stitteniptv.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.stitten.stitteniptv.database.dao.ErrorLogDao
import com.stitten.stitteniptv.database.dao.ParentalBlockDao
import com.stitten.stitteniptv.database.dao.WatchHistoryDao
import com.stitten.stitteniptv.database.entity.ErrorLogEntity
import com.stitten.stitteniptv.database.entity.ParentalBlockEntity
import com.stitten.stitteniptv.database.entity.WatchHistoryEntity

@Database(
    entities = [
        WatchHistoryEntity::class,
        ErrorLogEntity::class,
        ParentalBlockEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun errorLogDao(): ErrorLogDao
    abstract fun parentalBlockDao(): ParentalBlockDao
}
