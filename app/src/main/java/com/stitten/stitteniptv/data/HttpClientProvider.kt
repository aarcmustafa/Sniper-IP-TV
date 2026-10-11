package com.stitten.stitteniptv.data

import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.Interceptor
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

    // ═══════════════════════════════════════════════════════════
    //  User-Agent
    // ═══════════════════════════════════════════════════════════

    /** قائمة User-Agents الجاهزة */
    val USER_AGENT_PRESETS = listOf(
        "IPTVSmartersPro/1.1.1",
        "VLC/3.0.18 LibVLC/3.0.18",
        "TiviMate/4.7.0",
        "Kodi/20.0 (Linux; Android 12) ExoPlayerLib/2.19.1",
        "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"
    )

    const val DEFAULT_USER_AGENT = "IPTVSmartersPro/1.1.1"

    /** مرجع PrefsManager — يُمرَّر من Application */
    @Volatile
    private var prefsRef: PrefsManager? = null

    /** تهيئة المزوّد — استدعِها في Application.onCreate() */
    fun init(prefs: PrefsManager) {
        prefsRef = prefs
    }

    /** جلب UA الحالي أو الافتراضي */
    fun getUserAgent(): String {
        return try {
            prefsRef?.getUserAgent()?.takeIf { it.isNotBlank() } ?: DEFAULT_USER_AGENT
        } catch (_: Exception) {
            DEFAULT_USER_AGENT
        }
    }

    /**
     * Interceptor يضيف User-Agent + رؤوس موحّدة لكل الطلبات تلقائيًا.
     */
    private val uaInterceptor = Interceptor { chain ->
        val original = chain.request()
        val request: Request = original.newBuilder()
            .header("User-Agent", getUserAgent())
            .header("Accept", "*/*")
            .header("Connection", "keep-alive")
            .header("Accept-Encoding", "identity")
            .build()
        chain.proceed(request)
    }

    // ═══════════════════════════════════════════════════════════
    //  DNS Cache مخصص
    // ═══════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════
    //  Trust Manager (لقبول شهادات IPTV)
    // ═══════════════════════════════════════════════════════════

    private fun buildTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    private fun buildSslContext(): SSLContext? {
        return try {
            val tm = buildTrustManager()
            val ctx = SSLContext.getInstance("SSL")
            ctx.init(null, arrayOf<TrustManager>(tm), java.security.SecureRandom())
            ctx
        } catch (_: Exception) {
            null
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  عميل API
    // ═══════════════════════════════════════════════════════════

    val trustAllClient: OkHttpClient by lazy {
        val sslContext = buildSslContext()
        val tm = buildTrustManager()
        OkHttpClient.Builder()
            .addInterceptor(uaInterceptor)   // ← ✅
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

    // ═══════════════════════════════════════════════════════════
    //  عميل البث
    // ═══════════════════════════════════════════════════════════

    val streamingClient: OkHttpClient by lazy {
        val sslContext = buildSslContext()
        val tm = buildTrustManager()
        OkHttpClient.Builder()
            .addInterceptor(uaInterceptor)   // ← ✅
            .connectionPool(ConnectionPool(32, 15, TimeUnit.MINUTES))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext!!.socketFactory, tm)
            .hostnameVerifier { _, _ -> true }
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .socketFactory(object : javax.net.SocketFactory() {
                override fun createSocket(): java.net.Socket {
                    val socket = java.net.Socket()
                    socket.tcpNoDelay = true
                    return socket
                }
                override fun createSocket(host: String?, port: Int): java.net.Socket {
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
            .dns(CachedDns)
            .pingInterval(25, TimeUnit.SECONDS)
            .build()
    }

    // ═══════════════════════════════════════════════════════════
    //  Pre-warm (تسريع بدء البث)
    // ═══════════════════════════════════════════════════════════

    suspend fun prewarmConnection(url: String) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .head()
                    .build()
                streamingClient.newCall(request).execute().close()
            } catch (_: Exception) { }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  fetchText
    // ═══════════════════════════════════════════════════════════

    suspend fun fetchText(url: String): Result<String> {
        return try {
            val request = Request.Builder()
                .url(url)
                .build()   // الرؤوس تُضاف تلقائيًا من الـ interceptor
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
