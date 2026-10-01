package com.stitten.stitteniptv.data

import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object HttpClientProvider {

    // ============== DNS Cache مخصص ==============
    /**
     * ذاكرة DNS مؤقتة:
     * - تحفظ عنوان IP لكل نطاق لمدة ساعة
     * - تفادي DNS lookup المتكرر
     */
    private val dnsCache = ConcurrentHashMap<String, List<InetAddress>>()
    private val dnsTimestamps = ConcurrentHashMap<String, Long>()
    private const val DNS_CACHE_MS = 60 * 60 * 1000L // ساعة

    private object CachedDns : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val now = System.currentTimeMillis()
            val cached = dnsCache[hostname]
            val timestamp = dnsTimestamps[hostname] ?: 0L

            if (cached != null && (now - timestamp) < DNS_CACHE_MS) {
                return cached
            }

            return try {
                val result = Dns.SYSTEM.lookup(hostname)
                dnsCache[hostname] = result
                dnsTimestamps[hostname] = now
                result
            } catch (e: Exception) {
                cached ?: throw e
            }
        }
    }

    fun clearDnsCache() {
        dnsCache.clear()
        dnsTimestamps.clear()
    }

    // ============== Trust Manager ==============
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

    // ============== عميل API ==============
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
            .dns(CachedDns)
            .build()
    }

    // ============== عميل البث (محسّن للـ IPTV) ==============
    /**
     * إعدادات مبنية على تحليل PCAPdroid:
     * - ConnectionPool كبير (32 اتصال، 15 دقيقة)
     * - Keep-Alive طويل
     * - TCP NoDelay (بدون تأخير)
     * - DNS Cache مخصص
     * - Ping للحفاظ على الاتصال
     */
    val streamingClient: OkHttpClient by lazy {
        val sslContext = buildSslContext()
        val tm = buildTrustManager()
        OkHttpClient.Builder()
            // Connection Pool ضخم
            .connectionPool(ConnectionPool(32, 15, TimeUnit.MINUTES))
            // مهلات متوازنة
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS) // بلا حد للبث المستمر
            // SSL
            .sslSocketFactory(sslContext!!.socketFactory, tm)
            .hostnameVerifier { _, _ -> true }
            // إعادة التوجيه
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            // Socket Factory محسّن (TCP_NODELAY)
            .socketFactory(object : javax.net.SocketFactory() {
                override fun createSocket(): java.net.Socket {
                    val socket = java.net.Socket()
                    socket.tcpNoDelay = true // مهم للبث
                    return socket
                }
                override fun createSocket(
                    host: String?, port: Int
                ): java.net.Socket {
                    val socket = java.net.Socket(host, port)
                    socket.tcpNoDelay = true
                    return socket
                }
                override fun createSocket(
                    host: String?, port: Int,
                    localHost: java.net.InetAddress?, localPort: Int
                ): java.net.Socket {
                    val socket = java.net.Socket(host, port, localHost, localPort)
                    socket.tcpNoDelay = true
                    return socket
                }
                override fun createSocket(
                    host: java.net.InetAddress?, port: Int
                ): java.net.Socket {
                    val socket = java.net.Socket(host, port)
                    socket.tcpNoDelay = true
                    return socket
                }
                override fun createSocket(
                    address: java.net.InetAddress?, port: Int,
                    localAddress: java.net.InetAddress?, localPort: Int
                ): java.net.Socket {
                    val socket = java.net.Socket(address, port, localAddress, localPort)
                    socket.tcpNoDelay = true
                    return socket
                }
            })
            // DNS Cache
            .dns(CachedDns)
            // Ping للحفاظ على الاتصال
            .pingInterval(25, TimeUnit.SECONDS)
            .build()
    }

    fun getUserAgent(): String = "VLC/3.0.18 LibVLC/3.0.18"

    // ============== Pre-warm الاتصال ==============
    /**
     * يفتح اتصالاً مسبقاً بالسيرفر قبل بدء البث
     * يقلل زمن بدء التشغيل من 2-3 ثوانٍ إلى 0.5 ثانية
     */
    suspend fun prewarmConnection(url: String) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", getUserAgent())
                    .head()
                    .build()
                streamingClient.newCall(request).execute().close()
            } catch (e: Exception) { }
        }
    }

    // ============== fetchText ==============
    suspend fun fetchText(url: String): Result<String> {
        return try {
            val request = Request.Builder()
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
}
