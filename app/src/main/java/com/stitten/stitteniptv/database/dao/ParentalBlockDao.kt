package com.stitten.stitteniptv.database.dao

import androidx.room.*
import com.stitten.stitteniptv.database.entity.ParentalBlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParentalBlockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ParentalBlockEntity)

    @Delete
    suspend fun delete(item: ParentalBlockEntity)

    @Query("SELECT * FROM parental_blocks")
    fun getAll(): Flow<List<ParentalBlockEntity>>

    @Query("SELECT * FROM parental_blocks WHERE blockId = :id LIMIT 1")
    suspend fun getById(id: String): ParentalBlockEntity?

    @Query("DELETE FROM parental_blocks")
    suspend fun clearAll()
}
