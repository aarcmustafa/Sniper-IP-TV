package com.stitten.stitteniptv.player

/**
 * نوع محرك التشغيل المختار من الإعدادات.
 */
enum class EngineType {
    AUTO,       // يختار تلقائيًا حسب نوع الرابط
    EXOPLAYER,  // إجباري ExoPlayer (Media3)
    VLC         // إجباري libvlc
}
