package com.stitten.stitteniptv.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.stitten.stitteniptv.database.dao.ChannelDao
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.database.entity.EpisodeEntity
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

    // ═══════════════════════════════════════════════════════════
    //  حفظ القنوات
    // ═══════════════════════════════════════════════════════════
    suspend fun saveChannels(channels: List<Channel>, sourceId: Long = 0) {
        val packagePriorities = channels
            .groupBy { it.group }
            .mapValues { (pkg, chs) ->
                CategoriesManager.getPriorityForPackage(pkg, chs.map { it.name })
            }

        val entities = channels.map {
            ChannelEntity(
                id = "${sourceId}_${it.id}",
                name = it.name,
                logo = it.logo,
                url = it.url,
                groupName = it.group,
                groupPriority = packagePriorities[it.group] ?: 4,
                sourceId = sourceId
            )
        }
        entities.chunked(500).forEach { chunk ->
            dao.insertChannels(chunk)
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  حفظ الأفلام
    // ═══════════════════════════════════════════════════════════
    suspend fun saveMovies(movies: List<Movie>, sourceId: Long = 0) {
        val now = System.currentTimeMillis()
        val entities = movies.map {
            MovieEntity(
                id = "${sourceId}_${it.id}",
                name = it.name,
                poster = it.poster,
                url = it.url,
                category = it.category,
                sourceId = sourceId,
                backdrop = it.backdrop,
                plot = it.plot,
                rating = it.rating,
                year = it.year,
                duration = it.duration,
                genre = it.genre,
                cast = it.cast,
                director = it.director,
                containerExtension = it.containerExtension,
                addedAt = if (it.addedAt > 0L) it.addedAt else now,
            )
        }
        entities.chunked(500).forEach { chunk ->
            dao.insertMovies(chunk)
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  حفظ المسلسلات
    // ═══════════════════════════════════════════════════════════
    suspend fun saveSeries(series: List<Series>, sourceId: Long = 0) {
        val now = System.currentTimeMillis()
        val entities = series.map {
            SeriesEntity(
                id = "${sourceId}_${it.id}",
                name = it.name,
                poster = it.poster,
                category = it.category,
                sourceId = sourceId,
                backdrop = it.backdrop,
                plot = it.plot,
                rating = it.rating,
                year = it.year,
                genre = it.genre,
                cast = it.cast,
                director = it.director,
                episodeCount = it.episodeCount,
                seasonCount = it.seasonCount,
                addedAt = if (it.addedAt > 0L) it.addedAt else now,
            )
        }
        entities.chunked(500).forEach { chunk ->
            dao.insertSeries(chunk)
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  ✅ جديد — حفظ الحلقات
    // ═══════════════════════════════════════════════════════════
    suspend fun saveEpisodes(seriesId: String, episodes: List<Episode>) {
        val now = System.currentTimeMillis()
        val entities = episodes.map {
            EpisodeEntity(
                id = "${seriesId}_${it.id}",
                seriesId = seriesId,
                title = it.title,
                season = it.season,
                episode = it.episode,
                url = it.url,
                poster = it.poster,
                plot = it.plot,
                duration = it.duration,
                rating = it.rating,
                containerExtension = it.containerExtension,
                episodeId = it.episodeId,
                addedAt = now,
            )
        }
        entities.chunked(500).forEach { chunk ->
            dao.insertEpisodes(chunk)
        }
    }

    /** جلب الحلقات المحفوظة (كموديل) */
    suspend fun getEpisodesBySeries(seriesId: String): List<Episode> {
        return dao.getEpisodesBySeries(seriesId).map { entity ->
            Episode(
                id = entity.id,
                title = entity.title,
                season = entity.season,
                episode = entity.episode,
                url = entity.url,
                poster = entity.poster,
                plot = entity.plot,
                duration = entity.duration,
                rating = entity.rating,
                containerExtension = entity.containerExtension,
                episodeId = entity.episodeId,
            )
        }
    }

    /** حذف حلقات مسلسل معين */
    suspend fun deleteEpisodesBySeries(seriesId: String) {
        dao.deleteEpisodesBySeries(seriesId)
    }

    // ═══════════════════════════════════════════════════════════
    //  القنوات — Paging
    // ═══════════════════════════════════════════════════════════
    fun getAllChannelsPaged(): Flow<PagingData<ChannelEntity>> =
        Pager(config = pagingConfig()) { dao.getAllChannels() }.flow

    fun getChannelsByGroupPaged(group: String): Flow<PagingData<ChannelEntity>> =
        Pager(config = pagingConfig()) { dao.getChannelsByGroup(group) }.flow

    fun searchChannelsPaged(query: String): Flow<PagingData<ChannelEntity>> =
        Pager(config = pagingConfig()) { dao.searchChannels(query) }.flow

    fun searchChannelsInGroupPaged(group: String, query: String): Flow<PagingData<ChannelEntity>> =
        Pager(config = pagingConfig()) { dao.searchChannelsInGroup(group, query) }.flow

    // ═══════════════════════════════════════════════════════════
    //  الأفلام — Paging
    // ═══════════════════════════════════════════════════════════
    fun getAllMoviesPaged(): Flow<PagingData<MovieEntity>> =
        Pager(config = pagingConfig()) { dao.getAllMovies() }.flow

    fun searchMoviesPaged(query: String): Flow<PagingData<MovieEntity>> =
        Pager(config = pagingConfig()) { dao.searchMovies(query) }.flow

    fun getLatestMoviesPaged(): Flow<PagingData<MovieEntity>> =
        Pager(config = pagingConfig()) { dao.getLatestMoviesPaged() }.flow

    fun getMoviesByCategoryPaged(category: String): Flow<PagingData<MovieEntity>> =
        Pager(config = pagingConfig()) { dao.getMoviesByCategory(category) }.flow

    fun getTopRatedMovies(limit: Int = 20): Flow<List<MovieEntity>> =
        dao.getTopRatedMovies(limit)

    fun getLatestMovies(limit: Int = 20): Flow<List<MovieEntity>> =
        dao.getLatestMovies(limit)

    suspend fun getMovieById(id: String): MovieEntity? = dao.getMovieById(id)

    // ═══════════════════════════════════════════════════════════
    //  المسلسلات — Paging
    // ═══════════════════════════════════════════════════════════
    fun getAllSeriesPaged(): Flow<PagingData<SeriesEntity>> =
        Pager(config = pagingConfig()) { dao.getAllSeries() }.flow

    fun searchSeriesPaged(query: String): Flow<PagingData<SeriesEntity>> =
        Pager(config = pagingConfig()) { dao.searchSeries(query) }.flow

    fun getSeriesByCategoryPaged(category: String): Flow<PagingData<SeriesEntity>> =
        Pager(config = pagingConfig()) { dao.getSeriesByCategory(category) }.flow

    suspend fun getSeriesById(id: String): SeriesEntity? = dao.getSeriesById(id)

    // ═══════════════════════════════════════════════════════════
    //  دفعات للشاشة الرئيسية
    // ═══════════════════════════════════════════════════════════
    suspend fun getFirstSeriesPage(limit: Int = 30): List<SeriesEntity> =
        dao.getSeriesBatch(limit)

    suspend fun getFirstChannelsPage(limit: Int = 30): List<ChannelEntity> =
        dao.getChannelsBatch(limit)

    suspend fun getChannelById(id: String): ChannelEntity? = dao.getChannelById(id)

    // ═══════════════════════════════════════════════════════════
    //  التصنيفات
    // ═══════════════════════════════════════════════════════════
    fun getChannelGroups(): Flow<List<String>> =
        dao.getAllChannelGroups().map { CategoriesManager.sortCategories(it) }

    fun getMovieCategories(): Flow<List<String>> = dao.getAllMovieCategories()
    fun getSeriesCategories(): Flow<List<String>> = dao.getAllSeriesCategories()

    // ═══════════════════════════════════════════════════════════
    //  العدّادات
    // ═══════════════════════════════════════════════════════════
    suspend fun getChannelCount() = dao.getChannelCount()
    suspend fun getMovieCount() = dao.getMovieCount()
    suspend fun getSeriesCount() = dao.getSeriesCount()
    suspend fun getEpisodeCount() = dao.getEpisodeCount()

    // ═══════════════════════════════════════════════════════════
    //  الحذف
    // ═══════════════════════════════════════════════════════════
    suspend fun clearAll() {
        dao.clearChannels()
        dao.clearMovies()
        dao.clearSeries()
        dao.clearEpisodes()
    }

    suspend fun deleteMovie(id: String) = dao.deleteMovieById(id)
    suspend fun deleteSeries(id: String) = dao.deleteSeriesById(id)
    suspend fun deleteMoviesBySource(sourceId: Long) = dao.deleteMoviesBySource(sourceId)
}
