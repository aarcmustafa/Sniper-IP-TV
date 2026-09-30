package com.stitten.stitteniptv.database.dao

import androidx.room.*
import com.stitten.stitteniptv.database.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {

    @Insert
    suspend fun insert(source: SourceEntity): Long

    @Update
    suspend fun update(source: SourceEntity)

    @Delete
    suspend fun delete(source: SourceEntity)

    @Query("SELECT * FROM sources ORDER BY createdAt DESC")
    fun getAll(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE type = :type ORDER BY createdAt DESC")
    fun getByType(type: String): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): SourceEntity?

    @Query("UPDATE sources SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE sources SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Query("SELECT * FROM sources WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SourceEntity?

    @Query("SELECT COUNT(*) FROM sources WHERE type = :type")
    suspend fun countByType(type: String): Int

    @Query("DELETE FROM sources")
    suspend fun clearAll()
}
