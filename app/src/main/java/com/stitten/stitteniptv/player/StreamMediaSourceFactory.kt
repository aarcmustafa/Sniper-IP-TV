package com.stitten.stitteniptv.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.hls.DefaultHlsDataSourceFactory

/**
 * Factory ذكي يختار نوع المصدر تلقائياً:
 * - HLS للروابط .m3u8
 * - Progressive للروابط .ts و .mp4
 *
 * ملاحظة مهمة: للمشكلة "Connection: close" على .ts:
 * نستخدم HLS MediaSource أيضاً للـ .ts لأنها تتعامل مع القطع المتعددة بشكل أفضل
 */
object StreamMediaSourceFactory {

    fun create(
        url: String,
        dataSourceFactory: DataSource.Factory
    ): MediaSource {
        val cleanUrl = url.substringBefore("?").lowercase()

        return when {
            // HLS واضح
            cleanUrl.contains(".m3u8") -> {
                buildHlsSource(url, dataSourceFactory)
            }

            // .ts مباشر → نجرّب HLS لأن السيرفرات عادة تدعم الاثنين
            cleanUrl.contains(".ts") -> {
                buildHlsSource(url.replace(".ts", ".m3u8"), dataSourceFactory)
                // ملاحظة: إذا فشل HLS، سنستخدم Progressive كـ fallback
            }

            // VOD عادي
            else -> {
                val mediaItem = MediaItem.fromUri(url)
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }

    /**
     * نسخة بديلة: Progressive للـ .ts (fallback إذا فشل HLS)
     */
    fun createProgressive(
        url: String,
        dataSourceFactory: DataSource.Factory
    ): MediaSource {
        val mediaItem = MediaItem.fromUri(url)
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
    }

    private fun buildHlsSource(
        url: String,
        dataSourceFactory: DataSource.Factory
    ): MediaSource {
        val hlsDataSourceFactory = DefaultHlsDataSourceFactory(dataSourceFactory)
        val mediaItem = MediaItem.fromUri(url)
        return HlsMediaSource.Factory(hlsDataSourceFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(mediaItem)
    }
}
