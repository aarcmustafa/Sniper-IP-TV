package com.sniper.iptv.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// نماذج البيانات المستخدمة
data class CategoryItem(
    val category_id: String = "",
    val category_name: String = ""
)

data class StreamItem(
    val num: Int = 0,
    val name: String = "",
    val stream_id: Int = 0,
    val stream_icon: String? = null,
    val category_id: String? = null
)

data class SeriesItem(
    val num: Int = 0,
    val name: String = "",
    val series_id: Int = 0,
    val cover: String? = null,
    val category_id: String? = null
)

// واجهة الاتصال بـ API عبر Retrofit
interface XtreamApiService {
    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<CategoryItem>

    @GET("player_api.php")
    suspend fun getVodCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): List<CategoryItem>

    @GET("player_api.php")
    suspend fun getSeriesCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): List<CategoryItem>

    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null
    ): List<StreamItem>

    @GET("player_api.php")
    suspend fun getVodStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: String? = null
    ): List<StreamItem>

    @GET("player_api.php")
    suspend fun getSeries(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series",
        @Query("category_id") categoryId: String? = null
    ): List<SeriesItem>

    @GET("player_api.php")
    suspend fun getSeriesInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: String
    ): Any
}

// كلاس إدارة طلبات السيرفر
class XtreamRepository(private val baseUrl: String) {

    private val apiService: XtreamApiService by lazy {
        val validUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        Retrofit.Builder()
            .baseUrl(validUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(XtreamApiService::class.java)
    }

    suspend fun getLiveCategories(username: String?, password: String?): List<CategoryItem> {
        val u = username ?: ""
        val p = password ?: ""
        return try {
            apiService.getLiveCategories(u, p)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getVodCategories(username: String?, password: String?): List<CategoryItem> {
        val u = username ?: ""
        val p = password ?: ""
        return try {
            apiService.getVodCategories(u, p)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeriesCategories(username: String?, password: String?): List<CategoryItem> {
        val u = username ?: ""
        val p = password ?: ""
        return try {
            apiService.getSeriesCategories(u, p)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLiveStreams(username: String?, password: String?, categoryId: String? = null): List<StreamItem> {
        val u = username ?: ""
        val p = password ?: ""
        return try {
            apiService.getLiveStreams(u, p, categoryId = categoryId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getVodStreams(username: String?, password: String?, categoryId: String? = null): List<StreamItem> {
        val u = username ?: ""
        val p = password ?: ""
        return try {
            apiService.getVodStreams(u, p, categoryId = categoryId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeries(username: String?, password: String?, categoryId: String? = null): List<SeriesItem> {
        val u = username ?: ""
        val p = password ?: ""
        return try {
            apiService.getSeries(u, p, categoryId = categoryId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeriesInfo(username: String?, password: String?, seriesId: String): Any? {
        val u = username ?: ""
        val p = password ?: ""
        return try {
            apiService.getSeriesInfo(u, p, seriesId = seriesId)
        } catch (e: Exception) {
            null
        }
    }
}
