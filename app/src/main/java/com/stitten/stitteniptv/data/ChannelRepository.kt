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
