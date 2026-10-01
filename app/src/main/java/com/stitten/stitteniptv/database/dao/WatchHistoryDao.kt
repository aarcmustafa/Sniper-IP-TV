package com.stitten.stitteniptv.database.dao

import androidx.room.*
import com.stitten.stitteniptv.database.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WatchHistoryEntity)

    @Query("SELECT * FROM watch_history ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE contentId = :id LIMIT 1")
    suspend fun getById(id: String): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE contentType = 'SERIES' AND seriesId = :seriesId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLastEpisodeOfSeries(seriesId: String): WatchHistoryEntity?

    @Query("DELETE FROM watch_history WHERE contentId = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()

    @Query("DELETE FROM watch_history WHERE updatedAt < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM watch_history")
    suspend fun getCount(): Int

    @Query("DELETE FROM watch_history WHERE contentId NOT IN (SELECT contentId FROM watch_history ORDER BY updatedAt DESC LIMIT :keepCount)")
    suspend fun keepOnlyRecent(keepCount: Int): Int
}
