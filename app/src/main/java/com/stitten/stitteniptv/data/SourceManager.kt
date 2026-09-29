package com.stitten.stitteniptv.data

import com.stitten.stitteniptv.database.dao.SourceDao
import com.stitten.stitteniptv.database.entity.SourceEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceManager @Inject constructor(
    private val dao: SourceDao
) {
    fun getAll(): Flow<List<SourceEntity>> = dao.getAll()

    suspend fun add(source: SourceEntity): Long = dao.insert(source)

    suspend fun update(source: SourceEntity) = dao.update(source)

    suspend fun delete(source: SourceEntity) = dao.delete(source)

    suspend fun getActive(): SourceEntity? = dao.getActive()

    suspend fun setActive(id: Long) {
        dao.clearActive()
        dao.setActive(id)
    }

    suspend fun getById(id: Long): SourceEntity? = dao.getById(id)

    suspend fun clearAll() = dao.clearAll()
}
