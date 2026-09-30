package com.stitten.stitteniptv.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.stitten.stitteniptv.database.dao.ChannelDao
import com.stitten.stitteniptv.database.dao.ErrorLogDao
import com.stitten.stitteniptv.database.dao.ParentalBlockDao
import com.stitten.stitteniptv.database.dao.SourceDao
import com.stitten.stitteniptv.database.dao.WatchHistoryDao
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.database.entity.ErrorLogEntity
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.database.entity.ParentalBlockEntity
import com.stitten.stitteniptv.database.entity.SeriesEntity
import com.stitten.stitteniptv.database.entity.SourceEntity
import com.stitten.stitteniptv.database.entity.WatchHistoryEntity

@Database(
    entities = [
        WatchHistoryEntity::class,
        ErrorLogEntity::class,
        ParentalBlockEntity::class,
        SourceEntity::class,
        ChannelEntity::class,
        MovieEntity::class,
        SeriesEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun errorLogDao(): ErrorLogDao
    abstract fun parentalBlockDao(): ParentalBlockDao
    abstract fun sourceDao(): SourceDao
    abstract fun channelDao(): ChannelDao
}
