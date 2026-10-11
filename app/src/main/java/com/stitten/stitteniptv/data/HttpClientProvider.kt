package com.stitten.stitteniptv.data

import android.content.Context
import okhttp3.ConnectionPool
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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

    private const val PREFS_NAME = "http_client_cookies"
    private const val KEY_COOKIES = "cookies"

    @Volatile
    private var appContext: Context? = null

    // ═══════════════════════════════════════════════════════════
    //  User-Agent
    // ═══════════════════════════════════════════════════════════

    val USER_AGENT_PRESETS = listOf(
        "IPTVSmartersPro/1.1.1",
        "VLC/3.0.18 LibVLC/3.0.18",
        "TiviMate/4.7.0",
        "Kodi/20.0 (Linux; Android 12) ExoPlayerLib/2.19.1",
        "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"
    )

    const val DEFAULT_USER_AGENT = "IPTVSmartersPro/1.1.1"

    @Volatile
    private var prefsRef: PrefsManager? = null

    /** ✅ تعديل: يستقبل Context + PrefsManager */
    fun init(context: Context, prefs: PrefsManager) {
        appContext = context.applicationContext
        prefsRef = prefs
        loadCookies()
    }

    fun getUserAgent(): String {
        return try {
            prefsRef?.getUserAgent()?.takeIf { it.isNotBlank() } ?: DEFAULT_USER_AGENT
        } catch (_: Exception) {
            DEFAULT_USER_AGENT
        }
    }

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
    //  CookieJar — حفظ دائم على القرص
    // ═══════════════════════════════════════════════════════════

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isEmpty()) return
            try {
                val key = url.host
                val list = cookieStore.getOrPut(key) { mutableListOf() }
                cookies.forEach { newCookie ->
                    list.removeAll {
                        it.name == newCookie.name && it.path == newCookie.path
                    }
                    // ✅ لا نحفظ الكوكي المنتهية
                    if (newCookie.expiresAt > System.currentTimeMillis() ||
                        newCookie.expiresAt == Long.MAX_VALUE
                    ) {
                        list.add(newCookie)
                    }
                }
                // ✅ حفظ على القرص
                saveCookies()
            } catch (_: Exception) { }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return try {
                val key = url.host
                val list = cookieStore[key] ?: return emptyList()
                val now = System.currentTimeMillis()
                list.filter { it.expiresAt > now || it.expiresAt == Long.MAX_VALUE }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /** حفظ كل الكوكيز في SharedPreferences */
    private fun saveCookies() {
        try {
            val ctx = appContext ?: return
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val all = mutableSetOf<String>()

            cookieStore.forEach { (_, list) ->
                list.forEach { cookie ->
                    val scheme = if (cookie.secure) "https" else "http"
                    val domain = cookie.domain.removePrefix(".")
                    val path = cookie.path.ifBlank { "/" }
                    val url = "$scheme://$domain$path"
                    all.add("$url|${cookie.toString()}")
                }
            }
            prefs.edit().putStringSet(KEY_COOKIES, all).apply()
        } catch (_: Exception) { }
    }

    /** تحميل الكوكيز من SharedPreferences */
    private fun loadCookies() {
        try {
            val ctx = appContext ?: return
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val set = prefs.getStringSet(KEY_COOKIES, emptySet()) ?: return
            val now = System.currentTimeMillis()

            cookieStore.clear()

            set.forEach { entry ->
                try {
                    val idx = entry.indexOf('|')
                    if (idx < 0) return@forEach
                    val url = entry.substring(0, idx)
                    val cookieStr = entry.substring(idx + 1)
                    val httpUrl = url.toHttpUrlOrNull() ?: return@forEach
                    val cookie = Cookie.parse(httpUrl, cookieStr) ?: return@forEach

                    // تجاهل الكوكي المنتهية
                    if (cookie.expiresAt < now && cookie.expiresAt != Long.MAX_VALUE) {
                        return@forEach
                    }

                    val key = httpUrl.host
                    val list = cookieStore.getOrPut(key) { mutableListOf() }
                    list.removeAll { it.name == cookie.name && it.path == cookie.path }
                    list.add(cookie)
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
    }

    /** مسح كل الكوكيز من الذاكرة + القرص */
    fun clearCookies() {
        try {
            cookieStore.clear()
            val ctx = appContext ?: return
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (_: Exception) { }
    }

    /** إعادة تحميل الكوكيز من القرص */
    fun reloadCookies() {
        loadCookies()
    }

    /**
     * يجمع كل الكوكيز الخاصة برابط معين في نص واحد.
     */
    fun getCookiesHeader(url: String): String? {
        return try {
            val httpUrl = url.toHttpUrlOrNull() ?: return null
            val cookies = cookieJar.loadForRequest(httpUrl)
            if (cookies.isEmpty()) null
            else cookies.joinToString("; ") { "${it.name}=${it.value}" }
        } catch (_: Exception) {
            null
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  DNS Cache مخصص
    // ═══════════════════════════════════════════════════════════

    private val dnsCache = ConcurrentHashMap<String, List<InetAddress>>()
    private val dnsTimestamps = ConcurrentHashMap<String, Long>()
    private const val DNS_CACHE_MS = 60 * 60 * 1000L

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
    //  Trust Manager
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
            .addInterceptor(uaInterceptor)
            .cookieJar(cookieJar)
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
            .addInterceptor(uaInterceptor)
            .cookieJar(cookieJar)
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
    //  Pre-warm
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
