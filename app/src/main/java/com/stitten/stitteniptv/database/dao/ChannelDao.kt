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

    @Query("SELECT * FROM channels ORDER BY name ASC")
    fun getAllChannels(): PagingSource<Int, ChannelEntity>

    @Query("SELECT * FROM channels WHERE groupName = :group ORDER BY name ASC")
    fun getChannelsByGroup(group: String): PagingSource<Int, ChannelEntity>

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchChannels(query: String): PagingSource<Int, ChannelEntity>

    @Query("SELECT DISTINCT groupName FROM channels WHERE groupName != '' ORDER BY groupName ASC")
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

    @Query("SELECT * FROM movies WHERE category = :cat ORDER BY name ASC")
    fun getMoviesByCategory(cat: String): PagingSource<Int, MovieEntity>

    @Query("SELECT * FROM movies WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchMovies(query: String): PagingSource<Int, MovieEntity>

    @Query("SELECT DISTINCT category FROM movies WHERE category != '' ORDER BY category ASC")
    fun getAllMovieCategories(): Flow<List<String>>

    @Query("DELETE FROM movies")
    suspend fun clearMovies()
    
    // ============== المسلسلات ==============
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: List<SeriesEntity>)

    @Query("SELECT * FROM series ORDER BY name ASC")
    fun getAllSeries(): PagingSource<Int, SeriesEntity>

    @Query("SELECT * FROM series WHERE category = :cat ORDER BY name ASC")
    fun getSeriesByCategory(cat: String): PagingSource<Int, SeriesEntity>

    @Query("SELECT * FROM series WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchSeries(query: String): PagingSource<Int, SeriesEntity>

    @Query("SELECT DISTINCT category FROM series WHERE category != '' ORDER BY category ASC")
    fun getAllSeriesCategories(): Flow<List<String>>

    @Query("DELETE FROM series")
    suspend fun clearSeries()

    // ============== التنظيف حسب المصدر ==============
    @Query("DELETE FROM channels WHERE sourceId = :sourceId")
    suspend fun clearChannelsBySource(sourceId: Long)

    @Query("DELETE FROM movies WHERE sourceId = :sourceId")
    suspend fun clearMoviesBySource(sourceId: Long)

    @Query("DELETE FROM series WHERE sourceId = :sourceId")
    suspend fun clearSeriesBySource(sourceId: Long)

    // ============== مسح كل شيء ==============
    @Query("DELETE FROM channels")
    suspend fun clearAllChannels()

    @Query("DELETE FROM movies")
    suspend fun clearAllMovies()

    @Query("DELETE FROM series")
    suspend fun clearAllSeries()
}
