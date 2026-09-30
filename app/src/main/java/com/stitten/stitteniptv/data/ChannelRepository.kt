package com.stitten.stitteniptv.data

import com.stitten.stitteniptv.database.dao.ChannelDao
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.database.entity.SeriesEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    private val dao: ChannelDao
) {

    // ============== حفظ البيانات ==============
    suspend fun saveChannels(channels: List<Channel>, sourceId: Long = 0) {
        val entities = channels.map {
            ChannelEntity(
                id = "${sourceId}_${it.id}",
                name = it.name,
                logo = it.logo,
                url = it.url,
                groupName = it.group,
                sourceId = sourceId
            )
        }
        entities.chunked(500).forEach { chunk ->
            dao.insertChannels(chunk)
        }
    }

    suspend fun saveMovies(movies: List<Movie>, sourceId: Long = 0) {
        val entities = movies.map {
            MovieEntity(
                id = "${sourceId}_${it.id}",
                name = it.name,
                poster = it.poster,
                url = it.url,
                category = it.category,
                sourceId = sourceId
            )
        }
        entities.chunked(500).forEach { chunk ->
            dao.insertMovies(chunk)
        }
    }

    suspend fun saveSeries(series: List<Series>, sourceId: Long = 0) {
        val entities = series.map {
            SeriesEntity(
                id = "${sourceId}_${it.id}",
                name = it.name,
                poster = it.poster,
                category = it.category,
                sourceId = sourceId
            )
        }
        entities.chunked(500).forEach { chunk ->
            dao.insertSeries(chunk)
        }
    }

    // ============== قراءة القنوات ==============
    fun getAllChannels() = dao.getAllChannels()
    fun getChannelsByGroup(group: String) = dao.getChannelsByGroup(group)
    fun searchChannels(query: String) = dao.searchChannels(query)
    fun getChannelGroups(): Flow<List<String>> = dao.getAllChannelGroups()
    suspend fun getChannelCount() = dao.getChannelCount()

    // ============== قراءة الأفلام ==============
    fun getAllMovies() = dao.getAllMovies()
    fun getMoviesByCategory(cat: String) = dao.getMoviesByCategory(cat)
    fun searchMovies(query: String) = dao.searchMovies(query)
    fun getMovieCategories(): Flow<List<String>> = dao.getAllMovieCategories()

    // ============== قراءة المسلسلات ==============
    fun getAllSeries() = dao.getAllSeries()
    fun getSeriesByCategory(cat: String) = dao.getSeriesByCategory(cat)
    fun searchSeries(query: String) = dao.searchSeries(query)
    fun getSeriesCategories(): Flow<List<String>> = dao.getAllSeriesCategories()

    // ============== التنظيف ==============
    suspend fun clearAll() {
        dao.clearAllChannels()
        dao.clearAllMovies()
        dao.clearAllSeries()
    }

    suspend fun clearBySource(sourceId: Long) {
        dao.clearChannelsBySource(sourceId)
        dao.clearMoviesBySource(sourceId)
        dao.clearSeriesBySource(sourceId)
    }
}
