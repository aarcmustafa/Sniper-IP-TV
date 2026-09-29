package com.stitten.stitteniptv.database.dao

import androidx.room.*
import com.stitten.stitteniptv.database.entity.ErrorLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ErrorLogDao {

    @Insert
    suspend fun insert(log: ErrorLogEntity)

    @Query("SELECT * FROM error_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAll(): Flow<List<ErrorLogEntity>>

    @Query("DELETE FROM error_logs")
    suspend fun clearAll()
}
