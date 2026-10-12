package com.stitten.stitteniptv.player

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class ExoPlayerEngine(private val context: Context) : PlayerEngine {

    override val engineName = "ExoPlayer"

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private var onStateChanged: ((PlayerState) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    init {
        player.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(state: Int) {
                val s = when (state) {
                    Player.STATE_BUFFERING -> PlayerState.Buffering(0f)
                    Player.STATE_READY ->
                        if (player.isPlaying) PlayerState.Playing else PlayerState.Paused
                    Player.STATE_ENDED -> PlayerState.Ended
                    Player.STATE_IDLE -> PlayerState.Idle
                    else -> PlayerState.Idle
                }
                onStateChanged?.invoke(s)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onStateChanged?.invoke(
                    if (isPlaying) PlayerState.Playing else PlayerState.Paused
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                onError?.invoke(error.message ?: "ExoPlayer: خطأ غير معروف")
                onStateChanged?.invoke(PlayerState.Error)
            }
        })
    }

    override val isPlaying: Boolean get() = player.isPlaying
    override val durationMs: Long get() = player.duration.coerceAtLeast(0L)
    override val positionMs: Long get() = player.currentPosition

    override fun prepare(url: String) {
        onStateChanged?.invoke(PlayerState.Loading)
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
    }

    override fun play() { player.play() }
    override fun pause() { player.pause() }
    override fun stop() { player.stop() }
    override fun release() { player.release() }

    override fun setVolume(percent: Int) {
        player.volume = percent.coerceIn(0, 100) / 100f
    }

    override fun seekTo(positionMs: Long) { player.seekTo(positionMs) }

    override fun setOnStateChanged(callback: (PlayerState) -> Unit) {
        onStateChanged = callback
    }

    override fun setOnError(callback: (String) -> Unit) {
        onError = callback
    }

    @Composable
    override fun VideoSurface(modifier: Modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = this@ExoPlayerEngine.player
                    useController = false
                }
            },
            modifier = modifier
        )
    }
}
