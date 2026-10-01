package com.stitten.stitteniptv.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stitten.stitteniptv.data.*
import com.stitten.stitteniptv.database.entity.SourceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.EntryPointAccessors

data class DashboardUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val loadingProgress: Int = 0,
    val channelsCount: Int = 0,
    val currentSourceName: String = "",
    val currentSourceType: String = "",
    val error: String? = null,
    val dataVersion: Int = 0
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    // ============== المديرون ==============
    val prefs = PrefsManager(app)
    val favorites = FavoritesManager(app)
    val favoritesSync = FavoritesSyncManager(app)

    private val entryPoint = EntryPointAccessors.fromApplication(
        app.applicationContext,
        com.stitten.stitteniptv.ui.screens.PlayerEntryPoint::class.java
    )
    val channelRepo = entryPoint.channelRepository()
    val sourceMgr = entryPoint.sourceManager()
    private val historyMgr = entryPoint.watchHistoryManager()
    private val errorLogger = entryPoint.errorLogger()
    private val playerSettings = entryPoint.playerSettingsManager()

    // ============== الحالة ==============
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _favoritesVersion = MutableStateFlow(0)
    val favoritesVersion: StateFlow<Int> = _favoritesVersion.asStateFlow()

    // ============== init ==============
    init {
        XtreamApi.setPreferredProtocol(prefs.useHttps)
        XtreamApi.setPreferredFormat(prefs.streamFormat)
        channelRepo.liteMode = prefs.liteModeEnabled
    }

    private fun getChannelLimit(): Int =
        if (prefs.liteModeEnabled) 15000 else Int.MAX_VALUE

    // ==========================================
    //  تسجيل الدخول بـ Xtream
    // ==========================================
    fun loginXtream(
        server: String,
        user: String,
        pass: String,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            XtreamApi.setPreferredProtocol(prefs.useHttps)
            XtreamApi.setPreferredFormat(prefs.streamFormat)
            channelRepo.liteMode = prefs.liteModeEnabled

            _uiState.value = DashboardUiState(
                isLoading = true,
                loadingMessage = "🔐 جاري التحقق...",
                loadingProgress = 5
            )

            val valid = withContext(Dispatchers.IO) {
                XtreamApi.validate(server, user, pass)
            }

            if (!valid) {
                _uiState.value = DashboardUiState(error = "❌ بيانات غير صحيحة")
                onDone(false)
                return@launch
            }

            prefs.isLoggedIn = true
            prefs.loginType = "XTREAM"
            prefs.serverUrl = server
            prefs.username = user
            prefs.password = pass

            onDone(true)

            withContext(Dispatchers.IO) { channelRepo.clearAll() }

            loadChannelsOnly(server, user, pass)
        }
    }

    // ==========================================
    //  تسجيل الدخول بـ M3U
    // ==========================================
    fun loginM3u(
        url: String,
        content: String? = null,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = DashboardUiState(
                isLoading = true,
                loadingMessage = "📥 جاري تحميل القائمة...",
                loadingProgress = 10
            )

            val list = if (content != null) {
                M3uParser.loadFromContent(content)
            } else {
                M3uParser.loadFromUrl(url)
            }

            if (list.isEmpty()) {
                _uiState.value = DashboardUiState(error = "❌ لا توجد قنوات")
                onDone(false)
            } else {
                prefs.isLoggedIn = true
                prefs.loginType = "M3U"
                prefs.m3uUrl = url

                val limited = list.take(getChannelLimit())
                withContext(Dispatchers.IO) {
                    channelRepo.clearAll()
                    channelRepo.saveChannels(limited)
                }

                _uiState.value = DashboardUiState(
                    channelsCount = limited.size,
                    loadingProgress = 100
                )
                onDone(true)
            }
        }
    }

    // ==========================================
    //  تحميل القنوات فقط (Xtream)
    // ==========================================
    private suspend fun loadChannelsOnly(
        server: String,
        user: String,
        pass: String
    ) {
        _uiState.value = _uiState.value.copy(
            loadingMessage = "📺 جاري تحميل القنوات...",
            loadingProgress = 10
        )

        val live = withContext(Dispatchers.IO) {
            XtreamApi.loadLiveStreams(server, user, pass) { loaded ->
                val progress = (loaded / 500).coerceAtMost(80)
                _uiState.value = _uiState.value.copy(
                    loadingMessage = "📺 تحميل القنوات... ($loaded)",
                    loadingProgress = 10 + progress
                )
            }
        }

        val limited = live.take(getChannelLimit())
        withContext(Dispatchers.IO) { channelRepo.saveChannels(limited) }

        _uiState.value = _uiState.value.copy(
            channelsCount = limited.size,
            isLoading = false,
            loadingProgress = 100,
            loadingMessage = "✅ ${limited.size} قناة",
            dataVersion = _uiState.value.dataVersion + 1
        )
    }
    
// ==========================================
//  تبديل المصدر
// ==========================================
fun switchSource(source: SourceEntity) {
    viewModelScope.launch {
        XtreamApi.setPreferredProtocol(prefs.useHttps)
        XtreamApi.setPreferredFormat(prefs.streamFormat)
        channelRepo.liteMode = prefs.liteModeEnabled

        withContext(Dispatchers.IO) { channelRepo.clearAll() }
        sourceMgr.setActive(source.id)

        _uiState.value = DashboardUiState(
            isLoading = true,
            loadingMessage = "🔐 جاري الاتصال بـ ${source.name}...",
            loadingProgress = 5,
            currentSourceName = source.name,
            currentSourceType = source.type
        )

        prefs.isLoggedIn = true
        prefs.loginType = source.type
        prefs.serverUrl = source.url
        prefs.username = source.username
        prefs.password = source.password
        prefs.m3uUrl = source.url

        when (source.type) {
            "XTREAM" -> {
                val valid = withContext(Dispatchers.IO) {
                    XtreamApi.validate(source.url, source.username, source.password)
                }
                if (!valid) {
                    _uiState.value = DashboardUiState(
                        error = "❌ فشل الاتصال بـ ${source.name}",
                        currentSourceName = source.name,
                        currentSourceType = source.type
                    )
                    return@launch
                }
                loadChannelsOnly(source.url, source.username, source.password)
            }
            "M3U" -> loadM3uSource(source)
        }
    }
}

// ==========================================
//  تحميل مصدر M3U
// ==========================================
private suspend fun loadM3uSource(source: SourceEntity) {
    _uiState.value = _uiState.value.copy(
        loadingMessage = "📥 جاري تحميل M3U...",
        loadingProgress = 20
    )

    val channels = if (source.url.startsWith("content://")) {
        emptyList()
    } else {
        withContext(Dispatchers.IO) {
            M3uParser.loadFromUrlStreaming(
                ctx = getApplication(),
                url = source.url,
                onProgress = { downloaded, total ->
                    val progress = if (total > 0) {
                        ((downloaded * 100) / total).toInt().coerceIn(5, 95)
                    } else {
                        (downloaded / 2).toInt().coerceIn(5, 95)
                    }
                    _uiState.value = _uiState.value.copy(
                        loadingProgress = progress,
                        loadingMessage = if (total > 0)
                            "📥 تحميل... $downloaded / $total MB"
                        else
                            "📥 تحميل... $downloaded MB"
                    )
                }
            )
        }
    }

    if (channels.isEmpty()) {
        _uiState.value = DashboardUiState(
            error = "❌ فشل تحميل M3U: ${source.name}",
            currentSourceName = source.name,
            currentSourceType = source.type
        )
        return
    }

    val limited = channels.take(getChannelLimit())
    withContext(Dispatchers.IO) { channelRepo.saveChannels(limited) }

    _uiState.value = _uiState.value.copy(
        channelsCount = limited.size,
        isLoading = false,
        loadingProgress = 100,
        loadingMessage = "✅ ${limited.size} قناة",
        dataVersion = _uiState.value.dataVersion + 1
    )
}

// ==========================================
//  تحديث المصدر الحالي يدوياً
// ==========================================
fun refreshM3uSource() {
    viewModelScope.launch {
        val source = withContext(Dispatchers.IO) { sourceMgr.getActive() }
        if (source != null) {
            switchSource(source)
            return@launch
        }

        // احتياطي: استخدم الإعدادات المحفوظة
        when (prefs.loginType) {
            "M3U" -> {
                val url = prefs.m3uUrl
                if (url.isBlank()) return@launch

                withContext(Dispatchers.IO) { channelRepo.clearAll() }

                _uiState.value = DashboardUiState(
                    isLoading = true,
                    loadingMessage = "🔄 جاري التحديث...",
                    loadingProgress = 5
                )

                val list = M3uParser.loadFromUrl(url)
                val limited = list.take(getChannelLimit())

                withContext(Dispatchers.IO) {
                    channelRepo.saveChannels(limited)
                }

                _uiState.value = DashboardUiState(
                    channelsCount = limited.size,
                    loadingProgress = 100
                )
            }
            "XTREAM" -> {
                loadChannelsOnly(
                    prefs.serverUrl,
                    prefs.username,
                    prefs.password
                )
            }
        }
    }
}

// ==========================================
//  تحميل المحتوى المحفوظ عند فتح التطبيق
// ==========================================
fun loadCachedContent() {
    viewModelScope.launch {
        XtreamApi.setPreferredProtocol(prefs.useHttps)
        XtreamApi.setPreferredFormat(prefs.streamFormat)
        channelRepo.liteMode = prefs.liteModeEnabled

        val active = withContext(Dispatchers.IO) { sourceMgr.getActive() }
        if (active != null) {
            switchSource(active)
            return@launch
        }

        when (prefs.loginType) {
            "M3U" -> {
                if (prefs.m3uUrl.isBlank()) return@launch
                val list = M3uParser.loadFromUrl(prefs.m3uUrl)
                val limited = list.take(getChannelLimit())
                withContext(Dispatchers.IO) {
                    channelRepo.clearAll()
                    channelRepo.saveChannels(limited)
                }
                _uiState.value = DashboardUiState(
                    channelsCount = limited.size,
                    loadingProgress = 100
                )
            }
            "XTREAM" -> {
                if (prefs.serverUrl.isBlank()) return@launch
                loadChannelsOnly(
                    prefs.serverUrl,
                    prefs.username,
                    prefs.password
                )
            }
        }
    }
}
