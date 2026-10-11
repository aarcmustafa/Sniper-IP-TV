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
 * DataSource يعتمد على OkHttp بدل DefaultHttpDataSource.
 *
 * الفوائد:
 *  - User-Agent من HttpClientProvider تلقائيًا
 *  - Cookies من CookieJar (نفس المستخدم في API)
 *  - Referer + Origin + Accept-Language
 *  - Logging (في Debug)
 *  - Connection Pool مشترك مع بقية التطبيق
 *  - DNS Cache مخصص
 *  - Ping كل 25 ثانية
 *
 * المنطق:
 *  - يجرّب كل رابط في [candidates] بالترتيب
 *  - إذا فشل واحد → ينتقل للتالي
 */
@UnstableApi
class RetryHttpDataSource(
    private val candidates: List<String>,
    listener: TransferListener? = null,
) : BaseDataSource(true) {

    private var currentIndex = 0
    private var current: OkHttpDataSource? = null
    private var currentUri: String? = null
    private var bytesRemaining = 0L

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
                currentIndex = i
                currentUri = url
                bytesRemaining = len
                transferStarted(dataSpec)
                return len
            } catch (e: IOException) {
                lastError = e
                current?.close()
                current = null
            } catch (e: Exception) {
                // بعض الأخطاء قد تكون RuntimeException من OkHttp
                lastError = IOException(e.message, e)
                current?.close()
                current = null
            }
        }
        throw lastError ?: IOException("All stream candidates failed")
    }

    /**
     * يبني OkHttpDataSource يعتمد على streamingClient المشترك.
     * كل الرؤوس (UA, Cookies, Referer) تُضاف تلقائيًا من OkHttpClient.
     */
    private fun buildSource(): OkHttpDataSource {
        return OkHttpDataSource.Factory(HttpClientProvider.streamingClient)
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
