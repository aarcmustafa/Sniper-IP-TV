package com.stitten.stitteniptv.data

object ContentRepository {
    var channels: List<Channel> = emptyList()
    var movies: List<Movie> = emptyList()
    var series: List<Series> = emptyList()
    var episodes: Map<String, List<Episode>> = emptyMap()

    fun clear() {
        channels = emptyList()
        movies = emptyList()
        series = emptyList()
        episodes = emptyMap()
    }
}
