package com.stitten.stitteniptv.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun errorLogDao(): ErrorLogDao
    abstract fun parentalBlockDao(): ParentalBlockDao
    abstract fun sourceDao(): SourceDao
    abstract fun channelDao(): ChannelDao

    companion object {

        // ═══════════════════════════════════════════════════════════
        //  Migration 4 → 5: إضافة حقول جديدة لـ movies + series
        // ═══════════════════════════════════════════════════════════
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // ───────────── جدول movies ─────────────
                db.execSQL("ALTER TABLE movies ADD COLUMN backdrop TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN plot TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN rating TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN year TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN duration TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN genre TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN cast_list TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN director TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN containerExtension TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN addedAt INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_movies_addedAt ON movies(addedAt)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_movies_sourceId ON movies(sourceId)"
                )

                // ───────────── جدول series ─────────────
                db.execSQL("ALTER TABLE series ADD COLUMN backdrop TEXT")
                db.execSQL("ALTER TABLE series ADD COLUMN plot TEXT")
                db.execSQL("ALTER TABLE series ADD COLUMN rating TEXT")
                db.execSQL("ALTER TABLE series ADD COLUMN year TEXT")
                db.execSQL("ALTER TABLE series ADD COLUMN genre TEXT")
                db.execSQL("ALTER TABLE series ADD COLUMN cast_list TEXT")
                db.execSQL("ALTER TABLE series ADD COLUMN director TEXT")
                db.execSQL("ALTER TABLE series ADD COLUMN episodeCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE series ADD COLUMN seasonCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE series ADD COLUMN addedAt INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_series_addedAt ON series(addedAt)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_series_sourceId ON series(sourceId)"
                )
            }
        }

        /** كل الـ Migrations مرتبة */
        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_4_5
        )
    }
}
