package com.stitten.stitteniptv.data

import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object HttpClientProvider {

    val trustAllClient: OkHttpClient by lazy {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<X509Certificate>, authType: String
                ) {}

                override fun checkServerTrusted(
                    chain: Array<X509Certificate>, authType: String
                ) {}

                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .callTimeout(300, TimeUnit.SECONDS)
                .sslSocketFactory(
                    sslContext.socketFactory,
                    trustAllCerts[0] as X509TrustManager
                )
                .hostnameVerifier { _, _ -> true }
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .callTimeout(300, TimeUnit.SECONDS)
                .followRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    fun getUserAgent(): String = "VLC/3.0.18 LibVLC/3.0.18"

    suspend fun fetchText(url: String): Result<String> {
        val startTime = System.currentTimeMillis()
        val tag = detectTag(url)

        // تسجيل بدء الطلب
        try {
            XtreamLogger.logRequestStart(url, tag)
        } catch (e: Exception) { }

        return try {
            val request = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", getUserAgent())
                .header("Accept", "*/*")
                .header("Accept-Encoding", "identity")
                .header("Connection", "keep-alive")
                .build()

            val response = trustAllClient.newCall(request).execute()
            val elapsed = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                try {
                    XtreamLogger.logRequestSuccess(
                        url, tag, response.code, elapsed, body.length
                    )
                } catch (e: Exception) { }
                response.close()
                Result.success(body)
            } else {
                try {
                    XtreamLogger.logRequestFailure(
                        url, tag, "HTTP ${response.code} ${response.message}", elapsed
                    )
                } catch (e: Exception) { }
                response.close()
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            try {
                XtreamLogger.logRequestFailure(
                    url, tag, "${e.javaClass.simpleName}: ${e.message}", elapsed
                )
            } catch (inner: Exception) { }
            Result.failure(e)
        }
    }

    private fun detectTag(url: String): String {
        return when {
            url.contains("player_api.php") -> "API"
            url.contains("/live/") -> "LIVE"
            url.contains("/movie/") -> "VOD"
            url.contains("/series/") -> "SERIES"
            url.contains(".m3u") -> "M3U"
            else -> "HTTP"
        }
    }
}
