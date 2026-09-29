package com.stitten.stitteniptv.data

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesSyncManager @Inject constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stitten_fav_sync", Context.MODE_PRIVATE)

    // ============== القنوات ==============
    fun markChannel(name: String) {
        if (name.isBlank()) return
        prefs.edit().putBoolean("ch:${normalize(name)}", true).apply()
    }

    fun unmarkChannel(name: String) {
        if (name.isBlank()) return
        prefs.edit().remove("ch:${normalize(name)}").apply()
    }

    fun isChannelMarked(name: String): Boolean =
        prefs.getBoolean("ch:${normalize(name)}", false)

    fun getAllMarkedChannelNames(): Set<String> =
        prefs.all.filter { it.key.startsWith("ch:") && it.value == true }
            .keys.map { it.removePrefix("ch:") }.toSet()

    // ============== الأفلام ==============
    fun markMovie(name: String) {
        if (name.isBlank()) return
        prefs.edit().putBoolean("mv:${normalize(name)}", true).apply()
    }

    fun unmarkMovie(name: String) {
        if (name.isBlank()) return
        prefs.edit().remove("mv:${normalize(name)}").apply()
    }

    fun isMovieMarked(name: String): Boolean =
        prefs.getBoolean("mv:${normalize(name)}", false)

    fun getAllMarkedMovieNames(): Set<String> =
        prefs.all.filter { it.key.startsWith("mv:") && it.value == true }
            .keys.map { it.removePrefix("mv:") }.toSet()

    // ============== المسلسلات ==============
    fun markSeries(name: String) {
        if (name.isBlank()) return
        prefs.edit().putBoolean("sr:${normalize(name)}", true).apply()
    }

    fun unmarkSeries(name: String) {
        if (name.isBlank()) return
        prefs.edit().remove("sr:${normalize(name)}").apply()
    }

    fun isSeriesMarked(name: String): Boolean =
        prefs.getBoolean("sr:${normalize(name)}", false)

    fun getAllMarkedSeriesNames(): Set<String> =
        prefs.all.filter { it.key.startsWith("sr:") && it.value == true }
            .keys.map { it.removePrefix("sr:") }.toSet()

    // ============== المزامنة التلقائية ==============
    fun syncChannels(channels: List<Channel>, favorites: FavoritesManager): Int {
        var count = 0
        channels.forEach { ch ->
            if (isChannelMarked(ch.name) && !favorites.isChannelFavorite(ch.id)) {
                favorites.toggleChannel(ch.id)
                count++
            }
        }
        return count
    }

    fun syncMovies(movies: List<Movie>, favorites: FavoritesManager): Int {
        var count = 0
        movies.forEach { mv ->
            if (isMovieMarked(mv.name) && !favorites.isMovieFavorite(mv.id)) {
                favorites.toggleMovie(mv.id)
                count++
            }
        }
        return count
    }

    fun syncSeries(series: List<Series>, favorites: FavoritesManager): Int {
        var count = 0
        series.forEach { sr ->
            if (isSeriesMarked(sr.name) && !favorites.isSeriesFavorite(sr.id)) {
                favorites.toggleSeries(sr.id)
                count++
            }
        }
        return count
    }

    fun syncAll(
        channels: List<Channel>,
        movies: List<Movie>,
        series: List<Series>,
        favorites: FavoritesManager
    ): Int {
        return syncChannels(channels, favorites) +
            syncMovies(movies, favorites) +
            syncSeries(series, favorites)
    }

    // ============== تصدير / استيراد ==============
    fun exportAsText(): String {
        val sb = StringBuilder()
        sb.appendLine("# STTITEN FAVORITES EXPORT")
        sb.appendLine("# date: ${System.currentTimeMillis()}")
        getAllMarkedChannelNames().forEach { sb.appendLine("CH|$it") }
        getAllMarkedMovieNames().forEach { sb.appendLine("MV|$it") }
        getAllMarkedSeriesNames().forEach { sb.appendLine("SR|$it") }
        return sb.toString()
    }

    fun importFromText(content: String) {
        val editor = prefs.edit()
        content.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("CH|") -> editor.putBoolean(
                    "ch:${normalize(trimmed.removePrefix("CH|"))}", true
                )
                trimmed.startsWith("MV|") -> editor.putBoolean(
                    "mv:${normalize(trimmed.removePrefix("MV|"))}", true
                )
                trimmed.startsWith("SR|") -> editor.putBoolean(
                    "sr:${normalize(trimmed.removePrefix("SR|"))}", true
                )
            }
        }
        editor.apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun normalize(name: String): String =
        name.trim().lowercase().replace("\\s+".toRegex(), " ")
}
