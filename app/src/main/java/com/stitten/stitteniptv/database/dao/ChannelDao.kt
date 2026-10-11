package com.stitten.stitteniptv.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.database.entity.SeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    // ═══════════════════════════════════════════════════════════
    //  القنوات
    // ═══════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════
    //  الأفلام — الأساسي (كما كان)
    // ═══════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════
    //  ✅ جديد — استعلامات إضافية للأفلام
    // ═══════════════════════════════════════════════════════════

    /** أحدث الأفلام (الأكثر إضافةً) — للـ Hero + صف "أحدث الأفلام" */
    @Query("SELECT * FROM movies ORDER BY addedAt DESC, name ASC LIMIT :limit")
    fun getLatestMovies(limit: Int = 20): Flow<List<MovieEntity>>

    /** أحدث الأفلام كـ Paging (لقائمة كاملة) */
    @Query("SELECT * FROM movies ORDER BY addedAt DESC, name ASC")
    fun getLatestMoviesPaged(): PagingSource<Int, MovieEntity>

    /** فيلم واحد بواسطة ID — لشاشة التفاصيل */
    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1")
    suspend fun getMovieById(id: String): MovieEntity?

    /** فيلم واحد كـ Flow (للتحديث التلقائي) */
    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1")
    fun observeMovieById(id: String): Flow<MovieEntity?>

    /** الأفلام حسب التصنيف */
    @Query("SELECT * FROM movies WHERE category = :category ORDER BY addedAt DESC, name ASC")
    fun getMoviesByCategory(category: String): PagingSource<Int, MovieEntity>

    /** الأفلام الأعلى تقييمًا */
    @Query("""
        SELECT * FROM movies 
        WHERE rating IS NOT NULL AND rating != '' 
        ORDER BY CAST(rating AS REAL) DESC 
        LIMIT :limit
    """)
    fun getTopRatedMovies(limit: Int = 20): Flow<List<MovieEntity>>

    /** الأفلام حسب المصدر */
    @Query("SELECT * FROM movies WHERE sourceId = :sourceId ORDER BY addedAt DESC, name ASC")
    fun getMoviesBySource(sourceId: Long): PagingSource<Int, MovieEntity>

    /** كل التصنيفات للأفلام */
    @Query("SELECT DISTINCT category FROM movies WHERE category != '' ORDER BY category ASC")
    fun getAllMovieCategories(): Flow<List<String>>

    /** حذف فيلم واحد */
    @Query("DELETE FROM movies WHERE id = :id")
    suspend fun deleteMovieById(id: String)

    /** حذف أفلام مصدر معين */
    @Query("DELETE FROM movies WHERE sourceId = :sourceId")
    suspend fun deleteMoviesBySource(sourceId: Long)

    // ═══════════════════════════════════════════════════════════
    //  المسلسلات — الأساسي (كما كان)
    // ═══════════════════════════════════════════════════════════
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: List<SeriesEntity>)

    @Query("SELECT * FROM series ORDER BY name ASC")
    fun getAllSeries(): PagingSource<Int, SeriesEntity>

    @Query("SELECT * FROM series WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchSeries(query: String): PagingSource<Int, SeriesEntity>

    @Query("SELECT COUNT(*) FROM series")
    suspend fun getSeriesCount(): Int

    @Query("DELETE FROM series")
    suspend fun clearSeries()

    // ═══════════════════════════════════════════════════════════
    //  ✅ جديد — استعلامات إضافية للمسلسلات
    // ═══════════════════════════════════════════════════════════

    /** مسلسل واحد بواسطة ID */
    @Query("SELECT * FROM series WHERE id = :id LIMIT 1")
    suspend fun getSeriesById(id: String): SeriesEntity?

    /** مسلسل واحد كـ Flow */
    @Query("SELECT * FROM series WHERE id = :id LIMIT 1")
    fun observeSeriesById(id: String): Flow<SeriesEntity?>

    /** المسلسلات حسب التصنيف */
    @Query("SELECT * FROM series WHERE category = :category ORDER BY name ASC")
    fun getSeriesByCategory(category: String): PagingSource<Int, SeriesEntity>

    /** كل تصنيفات المسلسلات */
    @Query("SELECT DISTINCT category FROM series WHERE category != '' ORDER BY category ASC")
    fun getAllSeriesCategories(): Flow<List<String>>

    /** حذف مسلسل واحد */
    @Query("DELETE FROM series WHERE id = :id")
    suspend fun deleteSeriesById(id: String)
}
