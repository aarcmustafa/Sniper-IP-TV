package com.stitten.stitteniptv.player

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.ArrayList

class VlcPlayerEngine(private val context: Context) : PlayerEngine {

    override val engineName = "VLC"

    private val libVLC: LibVLC = LibVLC(context, ArrayList<String>().apply {
        add("--network-caching=1500")
        add("--live-caching=1500")
        add("--file-caching=1000")
        add("--avcodec-hw=any")
        add("--rtsp-tcp")
        add("--no-drop-late-frames")
        add("--no-skip-frames")
    })

    private var mediaPlayer: MediaPlayer? = null
    private var vlcVideoLayout: VLCVideoLayout? = null
    private var onStateChanged: ((PlayerState) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    override val isPlaying: Boolean get() = mediaPlayer?.isPlaying == true
    override val durationMs: Long get() = mediaPlayer?.length ?: 0L
    override val positionMs: Long get() = mediaPlayer?.time ?: 0L

    private fun ensurePlayer(): MediaPlayer {
        mediaPlayer?.let { return it }
        val mp = MediaPlayer(libVLC).apply {
            vlcVideoLayout?.let { attachViews(it, null, false, false) }
            setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Opening ->
                        onStateChanged?.invoke(PlayerState.Loading)
                    MediaPlayer.Event.Playing ->
                        onStateChanged?.invoke(PlayerState.Playing)
                    MediaPlayer.Event.Paused ->
                        onStateChanged?.invoke(PlayerState.Paused)
                    MediaPlayer.Event.Stopped ->
                        onStateChanged?.invoke(PlayerState.Idle)
                    MediaPlayer.Event.Buffering ->
                        onStateChanged?.invoke(PlayerState.Buffering(event.buffering))
                    MediaPlayer.Event.EndReached ->
                        onStateChanged?.invoke(PlayerState.Ended)
                    MediaPlayer.Event.EncounteredError -> {
                        onStateChanged?.invoke(PlayerState.Error)
                        onError?.invoke("فشل تشغيل القناة بواسطة VLC")
                    }
                }
            }
        }
        mediaPlayer = mp
        return mp
    }

    fun attachView(layout: VLCVideoLayout) {
        vlcVideoLayout = layout
        mediaPlayer?.detachViews()
        mediaPlayer?.attachViews(layout, null, false, false)
    }

    // ✅ الدالة الجديدة تدعم قائمة مرشحين (VLC يستخدم الأول)
    override fun prepare(candidates: List<String>, kind: StreamKind) {
        val url = candidates.firstOrNull() ?: return
        if (url.isBlank()) return
        onStateChanged?.invoke(PlayerState.Loading)

        val mp = ensurePlayer()
        val media = Media(libVLC, Uri.parse(url)).apply {
            setHWDecoderEnabled(true, false)
            addOption(":network-caching=1500")
            addOption(":live-caching=1500")
            addOption(":rtsp-tcp")
        }
        mp.media = media
        media.release()
    }

    override fun play() { mediaPlayer?.play() }
    override fun pause() { mediaPlayer?.pause() }
    override fun stop() { mediaPlayer?.stop() }

    override fun release() {
        mediaPlayer?.apply {
            stop()
            detachViews()
            release()
        }
        mediaPlayer = null
        vlcVideoLayout = null
        libVLC.release()
    }

    override fun setVolume(percent: Int) {
        mediaPlayer?.volume = percent.coerceIn(0, 100)
    }

    override fun seekTo(positionMs: Long) {
        mediaPlayer?.time = positionMs
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
                VLCVideoLayout(ctx).also { attachView(it) }
            },
            modifier = modifier,
            update = { layout ->
                if (layout !== vlcVideoLayout) attachView(layout)
            }
        )
    }
}
