package com.stitten.stitteniptv.data

import android.content.Context
import android.content.SharedPreferences

class FavoritesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stitten_favorites", Context.MODE_PRIVATE)

    // ============== القنوات ==============
    fun isChannelFavorite(id: String): Boolean =
        prefs.getBoolean("ch_$id", false)

    fun toggleChannel(id: String, name: String = ""): Boolean {
        val newValue = !isChannelFavorite(id)
        prefs.edit().putBoolean("ch_$id", newValue).apply()
        if (name.isNotBlank()) {
            if (newValue) {
                prefs.edit().putString("chn_$id", name).apply()
            } else {
                prefs.edit().remove("chn_$id").apply()
            }
        }
        return newValue
    }

    fun getFavoriteChannelIds(): Set<String> =
        prefs.all.filter { it.key.startsWith("ch_") && !it.key.startsWith("chn_") && it.value == true }
            .keys.map { it.removePrefix("ch_") }.toSet()

    fun getChannelNameFor(id: String): String =
        prefs.getString("chn_$id", "") ?: ""

    // ============== الأفلام ==============
    fun isMovieFavorite(id: String): Boolean =
        prefs.getBoolean("mv_$id", false)

    fun toggleMovie(id: String, name: String = ""): Boolean {
        val newValue = !isMovieFavorite(id)
        prefs.edit().putBoolean("mv_$id", newValue).apply()
        if (name.isNotBlank()) {
            if (newValue) {
                prefs.edit().putString("mvn_$id", name).apply()
            } else {
                prefs.edit().remove("mvn_$id").apply()
            }
        }
        return newValue
    }

    fun getFavoriteMovieIds(): Set<String> =
        prefs.all.filter { it.key.startsWith("mv_") && !it.key.startsWith("mvn_") && it.value == true }
            .keys.map { it.removePrefix("mv_") }.toSet()

    fun getMovieNameFor(id: String): String =
        prefs.getString("mvn_$id", "") ?: ""

    // ============== المسلسلات ==============
    fun isSeriesFavorite(id: String): Boolean =
        prefs.getBoolean("sr_$id", false)

    fun toggleSeries(id: String, name: String = ""): Boolean {
        val newValue = !isSeriesFavorite(id)
        prefs.edit().putBoolean("sr_$id", newValue).apply()
        if (name.isNotBlank()) {
            if (newValue) {
                prefs.edit().putString("srn_$id", name).apply()
            } else {
                prefs.edit().remove("srn_$id").apply()
            }
        }
        return newValue
    }

    fun getFavoriteSeriesIds(): Set<String> =
        prefs.all.filter { it.key.startsWith("sr_") && !it.key.startsWith("srn_") && it.value == true }
            .keys.map { it.removePrefix("sr_") }.toSet()

    fun getSeriesNameFor(id: String): String =
        prefs.getString("srn_$id", "") ?: ""

    // ============== جماعي ==============
    fun getAllFavoriteChannelNames(): List<String> {
        return getFavoriteChannelIds().mapNotNull {
            getChannelNameFor(it).ifBlank { null }
        }
    }

    fun getAllFavoriteMovieNames(): List<String> {
        return getFavoriteMovieIds().mapNotNull {
            getMovieNameFor(it).ifBlank { null }
        }
    }

    fun getAllFavoriteSeriesNames(): List<String> {
        return getFavoriteSeriesIds().mapNotNull {
            getSeriesNameFor(it).ifBlank { null }
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
