package com.stitten.stitteniptv.data

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object HttpClientProvider {

    private val connectionPool = ConnectionPool(
        maxIdleConnections = 10,
        keepAliveDuration = 5,
        timeUnit = TimeUnit.MINUTES
    )

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
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(180, TimeUnit.SECONDS)
                .connectionPool(connectionPool)
                .retryOnConnectionFailure(true)
                .dns(SmartDns)
                .protocols(listOf(
                    okhttp3.Protocol.HTTP_2,
                    okhttp3.Protocol.HTTP_1_1
                ))
                .sslSocketFactory(
                    sslContext.socketFactory,
                    trustAllCerts[0] as X509TrustManager
                )
                .hostnameVerifier { _, _ -> true }
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .callTimeout(180, TimeUnit.SECONDS)
                .connectionPool(connectionPool)
                .dns(SmartDns)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    fun getUserAgent(): String = "VLC/3.0.18 LibVLC/3.0.18"

    suspend fun fetchText(url: String): Result<String> {
        return try {
            val request = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", getUserAgent())
                .header("Accept", "*/*")
                .header("Accept-Encoding", "identity")
                .header("Connection", "keep-alive")
                .build()

            val response = trustAllClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                response.close()
                Result.success(body)
            } else {
                response.close()
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearDnsCache() = SmartDns.clearCache()
}
