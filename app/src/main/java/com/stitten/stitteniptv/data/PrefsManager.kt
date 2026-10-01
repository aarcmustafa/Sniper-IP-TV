package com.stitten.stitteniptv.data

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stitten_prefs", Context.MODE_PRIVATE)

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).apply()

    var loginType: String
        get() = prefs.getString("login_type", "") ?: ""
        set(value) = prefs.edit().putString("login_type", value).apply()

    var serverUrl: String
        get() = prefs.getString("server_url", "") ?: ""
        set(value) = prefs.edit().putString("server_url", value).apply()

    var username: String
        get() = prefs.getString("username", "") ?: ""
        set(value) = prefs.edit().putString("username", value).apply()

    var password: String
        get() = prefs.getString("password", "") ?: ""
        set(value) = prefs.edit().putString("password", value).apply()

    var m3uUrl: String
        get() = prefs.getString("m3u_url", "") ?: ""
        set(value) = prefs.edit().putString("m3u_url", value).apply()

    var is24Hour: Boolean
        get() = prefs.getBoolean("is_24h", true)
        set(value) = prefs.edit().putBoolean("is_24h", value).apply()

    var useInternalPlayer: Boolean
        get() = prefs.getBoolean("use_internal_player", true)
        set(value) = prefs.edit().putBoolean("use_internal_player", value).apply()

    var useHardwareDecoder: Boolean
        get() = prefs.getBoolean("use_hw_decoder", true)
        set(value) = prefs.edit().putBoolean("use_hw_decoder", value).apply()

    var parentalPin: String
        get() = prefs.getString("parental_pin", "") ?: ""
        set(value) = prefs.edit().putString("parental_pin", value).apply()

    var favoritesSyncEnabled: Boolean
        get() = prefs.getBoolean("fav_sync_enabled", true)
        set(value) = prefs.edit().putBoolean("fav_sync_enabled", value).apply()

    var externalPlayerPackage: String
        get() = prefs.getString("ext_pkg", "") ?: ""
        set(value) = prefs.edit().putString("ext_pkg", value).apply()

    var alwaysAskExternalPlayer: Boolean
        get() = prefs.getBoolean("ext_ask", true)
        set(value) = prefs.edit().putBoolean("ext_ask", value).apply()

    var useHttps: Boolean
        get() = prefs.getBoolean("use_https", false)
        set(value) = prefs.edit().putBoolean("use_https", value).apply()

    var streamFormat: String
        get() = prefs.getString("stream_format", "ts") ?: "ts"
        set(value) = prefs.edit().putString("stream_format", value).apply()

    // ============== Lite Mode ==============
    var liteModeEnabled: Boolean
        get() = prefs.getBoolean("lite_mode", false)
        set(value) = prefs.edit().putBoolean("lite_mode", value).apply()

    var autoDetectionDone: Boolean
        get() = prefs.getBoolean("auto_detection_done", false)
        set(value) = prefs.edit().putBoolean("auto_detection_done", value).apply()

    var deviceIsLowEnd: Boolean
        get() = prefs.getBoolean("device_is_low_end", false)
        set(value) = prefs.edit().putBoolean("device_is_low_end", value).apply()

    var deviceCapabilityInfo: String
        get() = prefs.getString("device_capability_info", "") ?: ""
        set(value) = prefs.edit().putString("device_capability_info", value).apply()

    var userDisabledLiteMode: Boolean
        get() = prefs.getBoolean("user_disabled_lite", false)
        set(value) = prefs.edit().putBoolean("user_disabled_lite", value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
