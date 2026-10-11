package com.stitten.stitteniptv.player

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import com.stitten.stitteniptv.data.HttpClientProvider
import java.io.IOException

@UnstableApi
class RetryHttpDataSource(
    private val candidates: List<String>,
    listener: TransferListener? = null,
) : BaseDataSource(true) {

    private var currentIndex = 0
    private var current: DefaultHttpDataSource? = null
    private var currentUri: String? = null
    private var bytesRemaining = 0L

    override fun open(dataSpec: DataSpec): Long {
        val ua = HttpClientProvider.getUserAgent()
        var lastError: IOException? = null

        val requested = dataSpec.uri.toString()
        val startIndex = candidates.indexOfFirst { requested.startsWith(it) }
            .takeIf { it >= 0 } ?: 0

        for (i in startIndex until candidates.size) {
            val url = candidates[i]
            try {
                val source = buildSource(ua, url)
                val newSpec = dataSpec.withUri(Uri.parse(url))
                val len = source.open(newSpec)
                current = source
                currentIndex = i
                currentUri = url
                bytesRemaining = len
                transferStarted(dataSpec)
                return len
            } catch (e: IOException) {
                lastError = e
                current?.close()
                current = null
            }
        }
        throw lastError ?: IOException("All stream candidates failed")
    }

    /**
     * يبني DefaultHttpDataSource مع:
     *  - User-Agent
     *  - Cookie header (من CookieJar)
     *  - Referer header (لتجاوز حجب بعض السيرفرات)
     *  - AllowCrossProtocolRedirects
     */
    private fun buildSource(ua: String, url: String): DefaultHttpDataSource {
        val headers = mutableMapOf<String, String>()

        // 🍪 Cookie header
        try {
            val cookieHeader = HttpClientProvider.getCookiesHeader(url)
            if (!cookieHeader.isNullOrBlank()) {
                headers["Cookie"] = cookieHeader
            }
        } catch (_: Exception) { }

        // 🔗 Referer header (بعض السيرفرات تطلبه)
        try {
            val host = Uri.parse(url).host
            if (!host.isNullOrBlank()) {
                headers["Referer"] = "http://$host/"
                headers["Origin"] = "http://$host"
            }
        } catch (_: Exception) { }

        return DefaultHttpDataSource.Factory()
            .setUserAgent(ua)
            .setDefaultRequestProperties(headers)
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
            .createDataSource()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return -1

        val read = current?.read(buffer, offset, length) ?: -1
        if (read > 0) {
            bytesRemaining -= read
            bytesTransferred(read)
        }
        return read
    }

    override fun getUri(): Uri? = currentUri?.let { Uri.parse(it) }

    override fun close() {
        try {
            current?.close()
        } finally {
            current = null
            currentUri = null
            bytesRemaining = 0L
            transferEnded()
        }
    }
}
