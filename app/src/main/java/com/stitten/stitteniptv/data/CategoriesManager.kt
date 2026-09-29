package com.stitten.stitteniptv.data

object CategoriesManager {

    fun extractChannelCategories(channels: List<Channel>): List<String> =
        channels.map { it.group }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    fun extractMovieCategories(movies: List<Movie>): List<String> =
        movies.map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    fun extractSeriesCategories(series: List<Series>): List<String> =
        series.map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
}
