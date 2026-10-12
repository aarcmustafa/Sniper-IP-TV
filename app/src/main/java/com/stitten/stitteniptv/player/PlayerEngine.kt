package com.stitten.stitteniptv.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * واجهة موحدة لكل محركات التشغيل (ExoPlayer / libvlc).
 * شاشات ui/player يجب أن تتعامل مع هذه الواجهة فقط.
 */
interface PlayerEngine {

    val engineName: String

    val isPlaying: Boolean
    val durationMs: Long
    val positionMs: Long

    fun prepare(url: String)
    fun play()
    fun pause()
    fun stop()
    fun release()
    fun setVolume(percent: Int)
    fun seekTo(positionMs: Long)

    fun setOnStateChanged(callback: (PlayerState) -> Unit)
    fun setOnError(callback: (String) -> Unit)

    /** كل محرك يعرض سطح الفيديو بطريقته الخاصة */
    @Composable
    fun VideoSurface(modifier: Modifier)
}
