package com.stitten.stitteniptv.data

import android.content.Context
import android.content.SharedPreferences
import com.stitten.stitteniptv.player.EngineType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerSettingsManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stitten_player", Context.MODE_PRIVATE)

    var preferredAudioLanguage: String
        get() = prefs.getString("audio_lang", "ar") ?: "ar"
        set(v) = prefs.edit().putString("audio_lang", v).apply()

    var preferredSubtitleLanguage: String
        get() = prefs.getString("sub_lang", "ar") ?: "ar"
        set(v) = prefs.edit().putString("sub_lang", v).apply()

    var subtitlesEnabled: Boolean
        get() = prefs.getBoolean("subs_enabled", false)
        set(v) = prefs.edit().putBoolean("subs_enabled", v).apply()

    var audioBoostEnabled: Boolean
        get() = prefs.getBoolean("audio_boost", false)
        set(v) = prefs.edit().putBoolean("audio_boost", v).apply()

    var autoFrameRateMatch: Boolean
        get() = prefs.getBoolean("auto_fps", true)
        set(v) = prefs.edit().putBoolean("auto_fps", v).apply()

    var bufferMs: Int
        get() = prefs.getInt("buffer_ms", 15000)
        set(v) = prefs.edit().putInt("buffer_ms", v).apply()

    // ═══════════════════════════════════════════════════════════
    //  نوع المحرك (ExoPlayer فقط الآن — تم إزالة VLC)
    //  ملاحظة: القيمة المحفوظة القديمة "vlc" تُهاجر تلقائيًا إلى "exo"
    // ═══════════════════════════════════════════════════════════

    /**
     * القيمة الخام المحفوظة.
     * - أي قيمة قديمة "vlc"/"libvlc" تُعتبر الآن "exo".
     * - القيم المدعومة فعليًا: "exo" أو "auto".
     */
    var engine: String
        get() {
            val raw = prefs.getString("engine", "auto") ?: "auto"
            return if (raw.lowercase() in setOf("vlc", "libvlc")) "exo" else raw
        }
        set(v) = prefs.edit().putString("engine", v).apply()

    /** ✅ قراءة/كتابة EngineType مباشرة */
    var engineType: EngineType
        get() = when (engine.lowercase()) {
            "exo", "exoplayer", "media3" -> EngineType.EXOPLAYER
            else -> EngineType.AUTO
        }
        set(value) {
            engine = when (value) {
                EngineType.EXOPLAYER -> "exo"
                EngineType.AUTO -> "auto"
            }
        }

    // ═══════════════════════════════════════════════════════════

    var externalPlayerPackage: String
        get() = prefs.getString("ext_pkg", "") ?: ""
        set(v) = prefs.edit().putString("ext_pkg", v).apply()

    var alwaysAskExternalPlayer: Boolean
        get() = prefs.getBoolean("ext_ask", true)
        set(v) = prefs.edit().putBoolean("ext_ask", v).apply()

    var favoritesSyncEnabled: Boolean
        get() = prefs.getBoolean("fav_sync", true)
        set(v) = prefs.edit().putBoolean("fav_sync", v).apply()
}
