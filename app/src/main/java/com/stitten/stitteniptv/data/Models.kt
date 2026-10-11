package com.stitten.stitteniptv.data

enum class ContentType { LIVE, VOD, SERIES, FAVORITES }

// ═══════════════════════════════════════════════════════════════
//  Channel — قناة مباشرة
// ═══════════════════════════════════════════════════════════════
data class Channel(
    val id: String,
    val name: String,
    val logo: String = "",
    val url: String,
    val group: String = "",
    val isFavorite: Boolean = false,

    val streamId: Int? = null,
    val streamIcon: String? = null,
    val epgChannelId: String? = null,
    val containerExtension: String? = null,
)

// ═══════════════════════════════════════════════════════════════
//  Movie — فيلم VOD
// ═══════════════════════════════════════════════════════════════
data class Movie(
    val id: String,
    val name: String,
    val poster: String = "",
    val url: String,
    val category: String = "",

    val backdrop: String? = null,
    val plot: String? = null,
    val rating: String? = null,
    val year: String? = null,
    val duration: String? = null,
    val genre: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val containerExtension: String? = null,
    val addedAt: Long = 0L,
    val streamId: Int? = null,
    val sourceId: Long = 0L,
)

// ═══════════════════════════════════════════════════════════════
//  Series — مسلسل
// ═══════════════════════════════════════════════════════════════
data class Series(
    val id: String,
    val name: String,
    val poster: String = "",
    val category: String = "",

    val backdrop: String? = null,
    val plot: String? = null,
    val rating: String? = null,
    val year: String? = null,
    val genre: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val episodeCount: Int = 0,
    val seasonCount: Int = 0,
    val addedAt: Long = 0L,
    val seriesId: Int? = null,
    val sourceId: Long = 0L,
    val duration: String? = null,
)

// ═══════════════════════════════════════════════════════════════
//  Episode — حلقة مسلسل
// ═══════════════════════════════════════════════════════════════
data class Episode(
    val id: String,
    val title: String,
    val season: Int,
    val episode: Int,
    val url: String,
    val poster: String = "",

    val plot: String? = null,
    val duration: String? = null,           // ✅ مضاف
    val rating: String? = null,
    val containerExtension: String? = null,
    val episodeId: Int? = null,
)

// ═══════════════════════════════════════════════════════════════
//  Category — تصنيف
// ═══════════════════════════════════════════════════════════════
data class Category(val id: String, val name: String)
