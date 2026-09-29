package com.stitten.stitteniptv.data

enum class ContentType { LIVE, VOD, SERIES }

data class Channel(
    val id: String,
    val name: String,
    val logo: String = "",
    val url: String,
    val group: String = "",
    val isFavorite: Boolean = false
)

data class Movie(
    val id: String,
    val name: String,
    val poster: String = "",
    val url: String,
    val category: String = ""
)

data class Series(
    val id: String,
    val name: String,
    val poster: String = "",
    val category: String = ""
)

data class Episode(
    val id: String,
    val title: String,
    val season: Int,
    val episode: Int,
    val url: String,
    val poster: String = ""
)

data class Category(val id: String, val name: String)
