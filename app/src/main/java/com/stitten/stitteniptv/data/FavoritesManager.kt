package com.stitten.stitteniptv.data

import android.content.Context
import android.content.SharedPreferences

class FavoritesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stitten_favorites", Context.MODE_PRIVATE)

    // ============== القنوات ==============
    fun isChannelFavorite(id: String): Boolean =
        prefs.getBoolean("ch_$id", false)

    fun toggleChannel(id: String): Boolean {
        val newValue = !isChannelFavorite(id)
        prefs.edit().putBoolean("ch_$id", newValue).apply()
        return newValue
    }

    fun getFavoriteChannelIds(): Set<String> =
        prefs.all.filter { it.key.startsWith("ch_") && it.value == true }
            .keys.map { it.removePrefix("ch_") }.toSet()

    // ============== الأفلام ==============
    fun isMovieFavorite(id: String): Boolean =
        prefs.getBoolean("mv_$id", false)

    fun toggleMovie(id: String): Boolean {
        val newValue = !isMovieFavorite(id)
        prefs.edit().putBoolean("mv_$id", newValue).apply()
        return newValue
    }

    fun getFavoriteMovieIds(): Set<String> =
        prefs.all.filter { it.key.startsWith("mv_") && it.value == true }
            .keys.map { it.removePrefix("mv_") }.toSet()

    // ============== المسلسلات ==============
    fun isSeriesFavorite(id: String): Boolean =
        prefs.getBoolean("sr_$id", false)

    fun toggleSeries(id: String): Boolean {
        val newValue = !isSeriesFavorite(id)
        prefs.edit().putBoolean("sr_$id", newValue).apply()
        return newValue
    }

    fun getFavoriteSeriesIds(): Set<String> =
        prefs.all.filter { it.key.startsWith("sr_") && it.value == true }
            .keys.map { it.removePrefix("sr_") }.toSet()

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
