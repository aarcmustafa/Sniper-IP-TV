package com.stitten.stitteniptv.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import com.stitten.stitteniptv.data.network.UserAgentProvider
import kotlinx.coroutines.runBlocking
import java.io.IOException

@UnstableApi
class RetryHttpDataSource(
    private val context: Context,
    private val candidates: List<String>,
    listener: TransferListener? = null,
) : BaseDataSource(true) {

    private var currentIndex = 0
    private var current: DefaultHttpDataSource? = null
    private var currentUri: String? = null
    private var bytesRemaining = 0L

    override fun open(dataSpec: DataSpec): Long {
        val ua = runBlocking { UserAgentProvider.get(context) }
        var lastError: IOException? = null

        val requested = dataSpec.uri.toString()
        val startIndex = candidates.indexOfFirst { requested.startsWith(it) }
            .takeIf { it >= 0 } ?: 0

        for (i in startIndex until candidates.size) {
            val url = candidates[i]
            try {
                val source = buildSource(ua)
                val newSpec = dataSpec.withUri(android.net.Uri.parse(url))
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

    private fun buildSource(ua: String): DefaultHttpDataSource =
        DefaultHttpDataSource.Factory()
            .setUserAgent(ua)
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
            .createDataSource()

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

    override fun getUri(): android.net.Uri? =
        currentUri?.let { android.net.Uri.parse(it) }

    override fun close() {
        try { current?.close() } finally {
            current = null
            currentUri = null
            bytesRemaining = 0L
            transferEnded()
        }
    }
}
