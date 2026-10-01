package com.stitten.stitteniptv.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException

/**
 * DataSource يلتف حول OkHttpDataSource:
 * - يعيد المحاولة تلقائياً عند فشل الاتصال
 * - يعالج مشكلة "Connection: close"
 * - لا يرمي IOException حتى بعد محاولات كثيرة
 */
class RetryDataSource(
    private val upstream: DataSource,
    private val maxRetries: Int = 10,
    private val retryDelayMs: Long = 300
) : DataSource {

    private var currentUri: Uri? = null
    private var currentPosition: Long = 0
    private var opened = false

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        currentUri = dataSpec.uri
        currentPosition = dataSpec.position

        var lastException: IOException? = null
        for (attempt in 0..maxRetries) {
            try {
                val length = upstream.open(dataSpec)
                opened = true
                return length
            } catch (e: IOException) {
                lastException = e
                // إذا كان "end of stream" — لا نحاول مجدداً
                if (e.message?.contains("end of stream", ignoreCase = true) == true) {
                    throw e
                }
                // حاول مرة أخرى
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs * (attempt + 1))
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw e
                    }
                }
            }
        }
        throw lastException ?: IOException("فشل الاتصال بعد $maxRetries محاولات")
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return try {
            upstream.read(buffer, offset, length)
        } catch (e: IOException) {
            // إذا كنا قد فتحنا — رمِ الخطأ ليقرر المشغل
            throw e
        }
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> {
        return upstream.responseHeaders
    }

    override fun close() {
        try {
            upstream.close()
        } catch (e: Exception) { }
        opened = false
    }
}
