package com.stitten.stitteniptv.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.stitten.stitteniptv.database.dao.ChannelDao
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.database.entity.SeriesEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    private val dao: ChannelDao
) {

    @Volatile
    var liteMode: Boolean = false

    private fun pagingConfig(): PagingConfig {
        return if (liteMode) {
            PagingConfig(
                pageSize = 15,
                prefetchDistance = 3,
                enablePlaceholders = false,
                maxSize = 60
            )
        } else {
            PagingConfig(
                pageSize = 50,
                prefetchDistance = 10,
                enablePlaceholders = false
            )
        }
    }

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
    
    fun getAllChannelsPaged(): Flow<PagingData<ChannelEntity>> =
        Pager(config = pagingConfig()) { dao.getAllChannels() }.flow

    fun getChannelsByGroupPaged(group: String): Flow<PagingData<ChannelEntity>> =
        Pager(config = pagingConfig()) { dao.getChannelsByGroup(group) }.flow

    fun searchChannelsPaged(query: String): Flow<PagingData<ChannelEntity>> =
        Pager(config = pagingConfig()) { dao.searchChannels(query) }.flow

    fun searchChannelsInGroupPaged(group: String, query: String): Flow<PagingData<ChannelEntity>> =
        Pager(config = pagingConfig()) { dao.searchChannelsInGroup(group, query) }.flow

    fun getAllMoviesPaged(): Flow<PagingData<MovieEntity>> =
        Pager(config = pagingConfig()) { dao.getAllMovies() }.flow

    fun getMoviesByCategoryPaged(cat: String): Flow<PagingData<MovieEntity>> =
        Pager(config = pagingConfig()) { dao.getMoviesByCategory(cat) }.flow

    fun searchMoviesPaged(query: String): Flow<PagingData<MovieEntity>> =
        Pager(config = pagingConfig()) { dao.searchMovies(query) }.flow

    fun searchMoviesInCategoryPaged(cat: String, query: String): Flow<PagingData<MovieEntity>> =
        Pager(config = pagingConfig()) { dao.searchMoviesInCategory(cat, query) }.flow

    fun getAllSeriesPaged(): Flow<PagingData<SeriesEntity>> =
        Pager(config = pagingConfig()) { dao.getAllSeries() }.flow

    fun getSeriesByCategoryPaged(cat: String): Flow<PagingData<SeriesEntity>> =
        Pager(config = pagingConfig()) { dao.getSeriesByCategory(cat) }.flow

    fun searchSeriesPaged(query: String): Flow<PagingData<SeriesEntity>> =
        Pager(config = pagingConfig()) { dao.searchSeries(query) }.flow

    fun searchSeriesInCategoryPaged(cat: String, query: String): Flow<PagingData<SeriesEntity>> =
        Pager(config = pagingConfig()) { dao.searchSeriesInCategory(cat, query) }.flow

    fun getChannelGroups(): Flow<List<String>> =
        dao.getAllChannelGroups().map { CategorySorter.sort(it) }

    fun getMovieCategories(): Flow<List<String>> =
        dao.getAllMovieCategories().map { CategorySorter.sort(it) }

    fun getSeriesCategories(): Flow<List<String>> =
        dao.getAllSeriesCategories().map { CategorySorter.sort(it) }

    suspend fun getChannelCount() = dao.getChannelCount()
    suspend fun getMovieCount() = dao.getMovieCount()
    suspend fun getSeriesCount() = dao.getSeriesCount()

    suspend fun clearAll() {
        dao.clearChannels()
        dao.clearMovies()
        dao.clearSeries()
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
