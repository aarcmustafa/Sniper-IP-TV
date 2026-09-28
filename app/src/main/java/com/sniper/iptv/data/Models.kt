package com.sniper.iptv.data

import java.util.UUID

data class XtreamProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val serverUrl: String,
    val username: String,
    val password: String
)

data class MediaItemModel(
    val id: String,
    val title: String,
    val streamUrl: String,
    val posterUrl: String?,
    val category: String,
    val type: MediaType,
    val extraId: Int = 0,
    val epgChannelId: String? = null
)

enum class MediaType { LIVE, MOVIE, SERIES }

data class SeriesSeasonModel(val seasonNum: Int, val episodes: List<EpisodeModel>)
data class EpisodeModel(val id: String, val title: String, val episodeNum: Int, val streamUrl: String)

data class EpgProgram(val title: String, val startTime: String, val stopTime: String)

data class AppSettings(
    val is24HourFormat: Boolean = true,
    val lowQualityMode: Boolean = false,
    val language: String = "ar",
    val parentalPin: String = "0000",
    val activeProfileId: String? = null
)

