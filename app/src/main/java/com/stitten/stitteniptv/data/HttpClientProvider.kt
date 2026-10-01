package com.stitten.stitteniptv.data

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object HttpClientProvider {

    // ============== SSL TrustAll ==============
    private fun buildTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<X509Certificate>, authType: String
            ) {}

            override fun checkServerTrusted(
                chain: Array<X509Certificate>, authType: String
            ) {}

            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    private fun buildSslContext(): SSLContext? {
        return try {
            val tm = buildTrustManager()
            val ctx = SSLContext.getInstance("SSL")
            ctx.init(null, arrayOf<TrustManager>(tm), java.security.SecureRandom())
            ctx
        } catch (e: Exception) {
            null
        }
    }

    // ============== عميل API (كما هو) ==============
    val trustAllClient: OkHttpClient by lazy {
        val sslContext = buildSslContext()
        val tm = buildTrustManager()
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .callTimeout(300, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext!!.socketFactory, tm)
            .hostnameVerifier { _, _ -> true }
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    // ============== عميل البث (محسّن) ==============
    /**
     * عميل مخصص للبث:
     * - ConnectionPool كبير (20 اتصال)
     * - Keep-Alive طويل (10 دقائق)
     * - مهلات قصيرة للاتصال (لتجنب التأخير)
     * - مهلات طويلة للقراءة (للبث المستمر)
     */
    val streamingClient: OkHttpClient by lazy {
        val sslContext = buildSslContext()
        val tm = buildTrustManager()
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(20, 10, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext!!.socketFactory, tm)
            .hostnameVerifier { _, _ -> true }
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .pingInterval(30, TimeUnit.SECONDS) // للحفاظ على الاتصال
            .build()
    }

    fun getUserAgent(): String = "VLC/3.0.18 LibVLC/3.0.18"

    // ============== حل 302 Redirect مسبقاً ==============
    /**
     * يقوم بطلب HEAD للحصول على الرابط النهائي
     * يُستخدم لتخزين الرابط المباشر وتجنب 302 في كل طلب
     */
    suspend fun resolveRedirect(url: String): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // إذا كان الرابط مباشراً بالفعل (IP)، لا نحتاج حل
                if (url.contains("://") && url.substringAfter("://").substringBefore("/").matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+.*"))) {
                    return@withContext url
                }

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", getUserAgent())
                    .head()
                    .build()

                val response = streamingClient.newCall(request).execute()
                val finalUrl = response.request.url.toString()
                response.close()

                if (finalUrl != url && finalUrl.isNotBlank()) {
                    finalUrl
                } else {
                    url
                }
            } catch (e: Exception) {
                url
            }
        }
    }
