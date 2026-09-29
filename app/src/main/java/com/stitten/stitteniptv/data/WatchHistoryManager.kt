package com.stitten.stitteniptv.data

import com.stitten.stitteniptv.database.dao.WatchHistoryDao
import com.stitten.stitteniptv.database.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHistoryManager @Inject constructor(
    private val dao: WatchHistoryDao
) {
    fun getRecent(): Flow<List<WatchHistoryEntity>> = dao.getRecent()

    suspend fun save(item: WatchHistoryEntity) = dao.upsert(item)

    suspend fun getById(id: String) = dao.getById(id)

    suspend fun getLastEpisodeOfSeries(seriesId: String) =
        dao.getLastEpisodeOfSeries(seriesId)

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun clearAll() = dao.clearAll()
}
