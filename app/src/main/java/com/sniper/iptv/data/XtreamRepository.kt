package com.sniper.iptv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamApiService {
    @GET("player_api.php")
    suspend fun getLiveStreams(@Query("username") u: String, @Query("password") p: String, @Query("action") action: String = "get_live_streams"): List<XtreamStreamDto>

    @GET("player_api.php")
    suspend fun getVodStreams(@Query("username") u: String, @Query("password") p: String, @Query("action") action: String = "get_vod_streams"): List<XtreamVodDto>

    @GET("player_api.php")
    suspend fun getSeries(@Query("username") u: String, @Query("password") p: String, @Query("action") action: String = "get_series"): List<XtreamSeriesDto>

    @GET("player_api.php")
    suspend fun getSeriesInfo(@Query("username") u: String, @Query("password") p: String, @Query("action") action: String = "get_series_info", @Query("series_id") id: Int): XtreamSeriesDetailDto

    @GET("player_api.php")
    suspend fun getShortEpg(@Query("username") u: String, @Query("password") p: String, @Query("action") action: String = "get_short_epg", @Query("stream_id") streamId: Int): EpgResponseDto
}

data class XtreamStreamDto(val name: String, val stream_id: Int, val stream_icon: String?, val category_id: String, val epg_channel_id: String?)
data class XtreamVodDto(val name: String, val stream_id: Int, val stream_icon: String?, val container_extension: String)
data class XtreamSeriesDto(val name: String, val series_id: Int, val cover: String?, val plot: String?)
data class XtreamSeriesDetailDto(val episodes: Map<String, List<XtreamEpisodeDto>>)
data class XtreamEpisodeDto(val id: String, val title: String, val episode_num: Int, val container_extension: String)
data class EpgResponseDto(val epg_listings: List<EpgListingDto>?)
data class EpgListingDto(val title: String, val start: String, val end: String)

class XtreamRepository(private val baseUrl: String) {
    private val api: XtreamApiService by lazy {
        Retrofit.Builder().baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .addConverterFactory(GsonConverterFactory.create()).build().create(XtreamApiService::class.java)
    }

    suspend fun fetchAll(u: String, p: String): Triple<List<MediaItemModel>, List<MediaItemModel>, List<MediaItemModel>> {
        return withContext(Dispatchers.IO) {
            try {
                val lives = api.getLiveStreams(u, p).map {
                    MediaItemModel(it.stream_id.toString(), it.name, "${baseUrl.trimEnd('/')}/live/$u/$p/${it.stream_id}.ts", it.stream_icon, "Live", MediaType.LIVE, extraId = it.stream_id)
                }
                val vods = api.getVodStreams(u, p).map {
                    MediaItemModel(it.stream_id.toString(), it.name, "${baseUrl.trimEnd('/')}/movie/$u/$p/${it.stream_id}.${it.container_extension}", it.stream_icon, "Movie", MediaType.MOVIE)
                }
                val series = api.getSeries(u, p).map {
                    MediaItemModel(it.series_id.toString(), it.name, it.cover, it.plot ?: "", "Series", MediaType.SERIES, extraId = it.series_id)
                }
                Triple(lives, vods, series)
            } catch (e: Exception) {
                Triple(emptyList(), emptyList(), emptyList())
            }
        }
    }

    suspend fun fetchChannelEpg(u: String, p: String, streamId: Int): List<EpgProgram> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getShortEpg(u, p, streamId = streamId)
                response.epg_listings?.map { EpgProgram(it.title, it.start, it.end) } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun fetchSeriesDetails(u: String, p: String, seriesId: Int): List<SeriesSeasonModel> {
        return withContext(Dispatchers.IO) {
            try {
                val info = api.getSeriesInfo(u, p, seriesId = seriesId)
                info.episodes.map { (seasonNumStr, episodesList) ->
                    val seasonNum = seasonNumStr.toIntOrNull() ?: 1
                    val eps = episodesList.map { ep ->
                        EpisodeModel(ep.id, ep.title, ep.episode_num, "${baseUrl.trimEnd('/')}/series/$u/$p/${ep.id}.${ep.container_extension}")
                    }
                    SeriesSeasonModel(seasonNum, eps)
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

