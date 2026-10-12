package com.stitten.stitteniptv.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * واجهة موحدة لكل محركات التشغيل (ExoPlayer / libvlc).
 */
interface PlayerEngine {

    val engineName: String

    val isPlaying: Boolean
    val durationMs: Long
    val positionMs: Long

    /** تشغيل قائمة مرشحين (روابط احتياطية) + نوع البث */
    fun prepare(candidates: List<String>, kind: StreamKind)

    /** اختصار لرابط واحد (افتراضيًا LIVE) */
    fun prepare(url: String) = prepare(listOf(url), StreamKind.LIVE)

    fun play()
    fun pause()
    fun stop()
    fun release()
    fun setVolume(percent: Int)
    fun seekTo(positionMs: Long)

    fun setOnStateChanged(callback: (PlayerState) -> Unit)
    fun setOnError(callback: (String) -> Unit)

    @Composable
    fun VideoSurface(modifier: Modifier)
}
