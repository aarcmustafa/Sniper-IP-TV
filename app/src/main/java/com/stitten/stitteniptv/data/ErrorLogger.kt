package com.stitten.stitteniptv.data

import com.stitten.stitteniptv.database.dao.ErrorLogDao
import com.stitten.stitteniptv.database.entity.ErrorLogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorLogger @Inject constructor(
    private val dao: ErrorLogDao
) {
    fun getAll(): Flow<List<ErrorLogEntity>> = dao.getAll()

    suspend fun log(url: String, message: String, code: Int = 0) {
        dao.insert(ErrorLogEntity(url = url, errorMessage = message, errorCode = code))
    }

    suspend fun clear() = dao.clearAll()
}
