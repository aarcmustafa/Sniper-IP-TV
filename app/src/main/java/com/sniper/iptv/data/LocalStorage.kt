package com.sniper.iptv.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocalStorage(context: Context) {
    private val prefs = context.getSharedPreferences("SniperIPTV_MasterPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveProfiles(profiles: List<XtreamProfile>) {
        prefs.edit().putString("profiles_list", gson.toJson(profiles)).apply()
    }

    fun loadProfiles(): MutableList<XtreamProfile> {
        val json = prefs.getString("profiles_list", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<XtreamProfile>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun saveSettings(s: AppSettings) {
        prefs.edit().apply {
            putBoolean("format24", s.is24HourFormat)
            putBoolean("lowQuality", s.lowQualityMode)
            putString("lang", s.language)
            putString("pin", s.parentalPin)
            putString("activeProfile", s.activeProfileId)
            apply()
        }
    }

    fun loadSettings(): AppSettings {
        return AppSettings(
            is24HourFormat = prefs.getBoolean("format24", true),
            lowQualityMode = prefs.getBoolean("lowQuality", false),
            language = prefs.getString("lang", "ar") ?: "ar",
            parentalPin = prefs.getString("pin", "0000") ?: "0000",
            activeProfileId = prefs.getString("activeProfile", null)
        )
    }

    fun saveFavorites(ids: Set<String>) {
        prefs.edit().putStringSet("favorites", ids).apply()
    }

    fun loadFavorites(): MutableSet<String> {
        return prefs.getStringSet("favorites", mutableSetOf()) ?: mutableSetOf()
    }
}

