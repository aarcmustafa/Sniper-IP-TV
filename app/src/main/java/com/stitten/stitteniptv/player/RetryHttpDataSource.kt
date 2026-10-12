package com.stitten.stitteniptv.player

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
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

    init {
        listener?.let { }
    }

    override fun open(dataSpec: DataSpec): Long {
        var lastError: IOException? = null

        val requested = dataSpec.uri.toString()
        val startIndex = candidates.indexOfFirst { requested.startsWith(it) }
            .takeIf { it >= 0 } ?: 0

        for (i in startIndex until candidates.size) {
            val url = candidates[i]
            try {
                val source = buildSource(url)
                val newSpec = dataSpec.withUri(Uri.parse(url))
                val len = source.open(newSpec)
                current = source
                currentIndex = i
                currentUri = url
                bytesRemaining = len
                transferStarted(dataSpec)
                return len
            } catch (e: HttpDataSource.InvalidResponseCodeException) {
                val code = e.responseCode

                // ✅ إيقاف الحلقة عند 502/503 — السيرفر رفض، لا فائدة من تجربة أخرى
                if (code == 502 || code == 503) {
                    current?.close()
                    current = null
                    throw IOException("Stream unavailable (HTTP $code)", e)
                }

                // ✅ إيقاف الحلقة عند 401/403 — رفض دائم
                if (code == 401 || code == 403) {
                    current?.close()
                    current = null
                    throw IOException("Access denied (HTTP $code)", e)
                }

                // باقي الأكواد (404، …) → جرّب المرشح التالي
                lastError = e
                current?.close()
                current = null
            } catch (e: HttpDataSource.HttpDataSourceException) {
                lastError = e
                current?.close()
                current = null
            } catch (e: IOException) {
                lastError = e
                current?.close()
                current = null
            } catch (e: Exception) {
                lastError = IOException(e.message, e)
                current?.close()
                current = null
            }
        }
        throw lastError ?: IOException("All stream candidates failed")
    }

    private fun buildSource(url: String): DefaultHttpDataSource {
        val ua = HttpClientProvider.getUserAgent()

        val headers = mutableMapOf<String, String>()
        headers["User-Agent"] = ua
        headers["Accept"] = "*/*"
        // ✅ لا Accept-Encoding → يسمح بالضغط
        // ✅ لا Referer / لا Origin → بعض اللوحات ترفضهما

        try {
            val cookieHeader = HttpClientProvider.getCookiesHeader(url)
            if (!cookieHeader.isNullOrBlank()) {
                headers["Cookie"] = cookieHeader
            }
        } catch (_: Exception) { }

        return DefaultHttpDataSource.Factory()
            .setUserAgent(ua)
            .setDefaultRequestProperties(headers)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)
            .createDataSource()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return -1

        val read = try {
            current?.read(buffer, offset, length) ?: -1
        } catch (_: Exception) {
            -1
        }

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
        } catch (_: Exception) {
        } finally {
            current = null
            currentUri = null
            bytesRemaining = 0L
            transferEnded()
        }
    }
}
