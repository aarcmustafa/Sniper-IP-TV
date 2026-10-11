package com.stitten.stitteniptv.data.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("user_agent_prefs")

object UserAgentProvider {

    val PRESETS = listOf(
        "IPTVSmartersPro/1.1.1",
        "VLC/3.0.18 LibVLC/3.0.18",
        "TiviMate/4.7.0",
        "Kodi/20.0 (Linux; Android 12) ExoPlayerLib/2.19.1",
        "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"
    )

    const val DEFAULT_UA = "IPTVSmartersPro/1.1.1"
    private val KEY_UA = stringPreferencesKey("user_agent")

    suspend fun get(context: Context): String =
        context.dataStore.data.map { it[KEY_UA] ?: DEFAULT_UA }.first()

    suspend fun set(context: Context, value: String) {
        context.dataStore.edit { it[KEY_UA] = value }
    }
}
