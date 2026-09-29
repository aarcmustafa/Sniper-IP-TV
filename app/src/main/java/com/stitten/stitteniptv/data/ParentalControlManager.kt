package com.stitten.stitteniptv.data

import com.stitten.stitteniptv.database.dao.ParentalBlockDao
import com.stitten.stitteniptv.database.entity.ParentalBlockEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentalControlManager @Inject constructor(
    private val dao: ParentalBlockDao
) {
    fun getAllBlocks(): Flow<List<ParentalBlockEntity>> = dao.getAll()

    suspend fun addBlock(id: String, type: String, label: String) =
        dao.insert(ParentalBlockEntity(id, type, label))

    suspend fun removeBlock(item: ParentalBlockEntity) = dao.delete(item)

    suspend fun isBlocked(id: String): Boolean = dao.getById(id) != null

    suspend fun clearAll() = dao.clearAll()
}
