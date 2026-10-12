package com.stitten.stitteniptv.player

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.stitten.stitteniptv.data.HttpClientProvider
import java.io.IOException

/**
 * DataSource يعتمد على OkHttpClient المشترك (streamingClient).
 *
 * المزايا التي يحصل عليها البث من هذا الدمج:
 *   - SmartDns (IPv4 preference + DoH fallback)
 *   - CookieJar مشترك مع طلبات Xtream API
 *   - ConnectionPool مع 32 keep-alive connection
 *   - tcpNoDelay لتقليل زمن الاستجابة
 *   - retryOn5xx تلقائي
 *   - User-Agent موحّد (VLC/3.0.20 LibVLC/3.0.20)
 *
 * يدعم أيضاً قائمة مرشحين (روابط احتياطية).
 */
@UnstableApi
class RetryHttpDataSource(
    private val candidates: List<String>,
    listener: TransferListener? = null,
) : BaseDataSource(true) {

    private var current: OkHttpDataSource? = null
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
                val source = buildSource()
                val newSpec = dataSpec.withUri(Uri.parse(url))
                val len = source.open(newSpec)
                current = source
                currentUri = url
                bytesRemaining = len
                transferStarted(dataSpec)
                return len
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

    /**
     * نستخدم streamingClient المشترك من HttpClientProvider.
     * كل شيء (UA, DNS, Cookies, Timeouts, Retry) مُدار مركزياً هناك.
     */
    private fun buildSource(): OkHttpDataSource {
        return OkHttpDataSource.Factory(HttpClientProvider.streamingClient)
            .setUserAgent(HttpClientProvider.getUserAgent())
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
