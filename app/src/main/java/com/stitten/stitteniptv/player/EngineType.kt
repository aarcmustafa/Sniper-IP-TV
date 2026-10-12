package com.stitten.stitteniptv.player

/**
 * نوع محرك التشغيل المختار من الإعدادات.
 * ملاحظة: VLC تمت إزالته — ExoPlayer هو المحرك الوحيد.
 */
enum class EngineType {
    AUTO,       // اختيار تلقائي (ExoPlayer)
    EXOPLAYER   // إجباري ExoPlayer (Media3)
}
