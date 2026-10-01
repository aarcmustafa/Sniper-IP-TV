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

    suspend fun cleanupOld(days: Int = 30): Int {
        val cutoff = System.currentTimeMillis() -
                (days.toLong() * 24 * 60 * 60 * 1000)
        return dao.deleteOlderThan(cutoff)
    }

    suspend fun keepOnlyRecent(keepCount: Int = 100): Int {
        return dao.keepOnlyRecent(keepCount)
    }

    suspend fun performFullCleanup(): Int {
        return try {
            val oldRemoved = cleanupOld(30)
            val excessRemoved = keepOnlyRecent(100)
            oldRemoved + excessRemoved
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getCount(): Int = dao.getCount()
}
