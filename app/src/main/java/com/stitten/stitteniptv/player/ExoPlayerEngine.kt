package com.stitten.stitteniptv.player

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@UnstableApi
class ExoPlayerEngine(private val context: Context) : PlayerEngine {

    override val engineName = "ExoPlayer"

    // ✅ State حتى تُحدَّث واجهة PlayerView عند تغيّر المشغل
    private var player by mutableStateOf<ExoPlayer?>(null)

    private var onStateChanged: ((PlayerState) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    private var lastPositionMs: Long = 0L

    override val isPlaying: Boolean get() = player?.isPlaying == true
    override val durationMs: Long get() = player?.duration?.coerceAtLeast(0L) ?: 0L
    override val positionMs: Long get() = player?.currentPosition ?: 0L

    override fun prepare(candidates: List<String>, kind: StreamKind) {
        if (candidates.isEmpty()) return
        onStateChanged?.invoke(PlayerState.Loading)

        // حرر المشغل القديم قبل إنشاء واحد جديد
        player?.release()

        // ✅ استخدم PlayerFactory الموجود لديك (Retry + Cache + LoadControl)
        val newPlayer = PlayerFactory.create(
            context = context,
            candidates = candidates,
            kind = kind,
            startPositionMs = lastPositionMs
        )

        newPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val s = when (state) {
                    Player.STATE_BUFFERING -> PlayerState.Buffering(0f)
                    Player.STATE_READY ->
                        if (newPlayer.isPlaying) PlayerState.Playing else PlayerState.Paused
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
                // PlayerFactory يتعامل مع إعادة المحاولة، لكن نُبلّغ الواجهة
                onError?.invoke(error.message ?: "ExoPlayer: خطأ غير معروف")
                onStateChanged?.invoke(PlayerState.Error)
            }
        })

        player = newPlayer
    }

    override fun play() { player?.play() }
    override fun pause() { player?.pause() }
    override fun stop() { player?.stop() }

    override fun release() {
        player?.release()
        player = null
    }

    override fun setVolume(percent: Int) {
        player?.volume = percent.coerceIn(0, 100) / 100f
    }

    override fun seekTo(positionMs: Long) {
        lastPositionMs = positionMs
        player?.seekTo(positionMs)
    }

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
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            update = { view ->
                // ✅ يُربط تلقائيًا عند تغيّر player
                view.player = this@ExoPlayerEngine.player
            },
            modifier = modifier
        )
    }
}
