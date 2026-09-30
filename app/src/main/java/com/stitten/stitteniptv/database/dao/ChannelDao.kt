package com.stitten.stitteniptv.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.database.entity.SeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    // ============== القنوات ==============
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("SELECT * FROM channels ORDER BY groupPriority ASC, groupName ASC, name ASC")
    fun getAllChannels(): PagingSource<Int, ChannelEntity>

    @Query("SELECT * FROM channels WHERE groupName = :group ORDER BY groupPriority ASC, name ASC")
    fun getChannelsByGroup(group: String): PagingSource<Int, ChannelEntity>

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' ORDER BY groupPriority ASC, groupName ASC, name ASC")
    fun searchChannels(query: String): PagingSource<Int, ChannelEntity>

    @Query("SELECT * FROM channels WHERE groupName = :group AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchChannelsInGroup(group: String, query: String): PagingSource<Int, ChannelEntity>

    @Query("SELECT DISTINCT groupName FROM channels WHERE groupName != '' ORDER BY groupPriority ASC, groupName ASC")
    fun getAllChannelGroups(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun getChannelCount(): Int

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    // ============== الأفلام ==============
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Query("SELECT * FROM movies ORDER BY name ASC")
    fun getAllMovies(): PagingSource<Int, MovieEntity>

    @Query("SELECT * FROM movies WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchMovies(query: String): PagingSource<Int, MovieEntity>

    @Query("SELECT COUNT(*) FROM movies")
    suspend fun getMovieCount(): Int

    @Query("DELETE FROM movies")
    suspend fun clearMovies()
