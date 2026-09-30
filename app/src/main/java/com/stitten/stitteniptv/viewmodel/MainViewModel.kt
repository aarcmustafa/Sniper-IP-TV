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
    val moviesCount: Int = 0,
    val seriesCount: Int = 0,
    val currentSourceName: String = "",
    val currentSourceType: String = "",
    val error: String? = null,
    val dataVersion: Int = 0,
    // حالات التحميل عند الطلب
    val channelsLoaded: Boolean = false,
    val moviesLoaded: Boolean = false,
    val seriesLoaded: Boolean = false,
    val loadingMovies: Boolean = false,
    val loadingSeries: Boolean = false
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = PrefsManager(app)
    val favorites = FavoritesManager(app)
    val favoritesSync = FavoritesSyncManager(app)

    private val entryPoint = EntryPointAccessors.fromApplication(
        app.applicationContext,
        com.stitten.stitteniptv.ui.screens.PlayerEntryPoint::class.java
    )
    val channelRepo = entryPoint.channelRepository()
    val sourceMgr = entryPoint.sourceManager()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _favoritesVersion = MutableStateFlow(0)
    val favoritesVersion: StateFlow<Int> = _favoritesVersion.asStateFlow()

    init {
        XtreamApi.setPreferredProtocol(prefs.useHttps)
        XtreamApi.setPreferredFormat(prefs.streamFormat)
        channelRepo.liteMode = prefs.liteModeEnabled
    }

    private fun getChannelLimit(): Int = if (prefs.liteModeEnabled) 15000 else Int.MAX_VALUE
    private fun getMovieLimit(): Int = if (prefs.liteModeEnabled) 10000 else Int.MAX_VALUE
    private fun getSeriesLimit(): Int = if (prefs.liteModeEnabled) 5000 else Int.MAX_VALUE

    // ============== تسجيل الدخول ==============
    fun loginXtream(server: String, user: String, pass: String, onDone: (Boolean) -> Unit) {
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

            // ✅ تحميل القنوات فقط — الأفلام والمسلسلات عند الطلب
            loadChannelsOnly(server, user, pass)
        }
    }

    // ============== تحميل القنوات فقط (سريع) ==============
    private suspend fun loadChannelsOnly(server: String, user: String, pass: String) {
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
            channelsLoaded = true,
            isLoading = false,
            loadingProgress = 100,
            loadingMessage = "✅ ${limited.size} قناة",
            dataVersion = _uiState.value.dataVersion + 1
        )
    }
    
// ============== تحميل الأفلام عند الطلب ==============
fun loadMoviesOnDemand() {
    // لا تُعد التحميل إذا كانت محمّلة
    if (_uiState.value.moviesLoaded || _uiState.value.loadingMovies) return

    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loadingMovies = true)

        _uiState.value = _uiState.value.copy(
            loadingMessage = "🎬 جاري تحميل الأفلام...",
            loadingProgress = 5,
            isLoading = true
        )

        try {
            val vod = withContext(Dispatchers.IO) {
                XtreamApi.loadVodStreams(
                    prefs.serverUrl, prefs.username, prefs.password
                ) { loaded ->
                    val progress = (loaded / 2000).coerceAtMost(90)
                    _uiState.value = _uiState.value.copy(
                        loadingMessage = "🎬 تحميل الأفلام... ($loaded)",
                        loadingProgress = 5 + progress
                    )
                }
            }

            if (vod.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    loadingMovies = false,
                    isLoading = false,
                    error = "لا توجد أفلام في هذا المصدر"
                )
                return@launch
            }

            val limited = vod.take(getMovieLimit())
            withContext(Dispatchers.IO) { channelRepo.saveMovies(limited) }

            _uiState.value = _uiState.value.copy(
                moviesCount = limited.size,
                moviesLoaded = true,
                loadingMovies = false,
                isLoading = false,
                loadingProgress = 100,
                loadingMessage = "✅ ${limited.size} فيلم",
                dataVersion = _uiState.value.dataVersion + 1
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                loadingMovies = false,
                isLoading = false,
                error = "فشل تحميل الأفلام: ${e.message}"
            )
        }
    }
}

// ============== تحميل المسلسلات عند الطلب ==============
fun loadSeriesOnDemand() {
    if (_uiState.value.seriesLoaded || _uiState.value.loadingSeries) return

    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loadingSeries = true)

        _uiState.value = _uiState.value.copy(
            loadingMessage = "📼 جاري تحميل المسلسلات...",
            loadingProgress = 5,
            isLoading = true
        )

        try {
            val srs = withContext(Dispatchers.IO) {
                XtreamApi.loadSeries(
                    prefs.serverUrl, prefs.username, prefs.password
                ) { loaded ->
                    val progress = (loaded / 1000).coerceAtMost(90)
                    _uiState.value = _uiState.value.copy(
                        loadingMessage = "📼 تحميل المسلسلات... ($loaded)",
                        loadingProgress = 5 + progress
                    )
                }
            }

            if (srs.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    loadingSeries = false,
                    isLoading = false,
                    error = "لا توجد مسلسلات في هذا المصدر"
                )
                return@launch
            }

            val limited = srs.take(getSeriesLimit())
            withContext(Dispatchers.IO) { channelRepo.saveSeries(limited) }

            _uiState.value = _uiState.value.copy(
                seriesCount = limited.size,
                seriesLoaded = true,
                loadingSeries = false,
                isLoading = false,
                loadingProgress = 100,
                loadingMessage = "✅ ${limited.size} مسلسل",
                dataVersion = _uiState.value.dataVersion + 1
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                loadingSeries = false,
                isLoading = false,
                error = "فشل تحميل المسلسلات: ${e.message}"
            )
        }
    }
}

// ============== تبديل المصادر ==============
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

private suspend fun loadM3uSource(source: SourceEntity) {
    _uiState.value = _uiState.value.copy(
        loadingMessage = "📥 جاري تحميل M3U...",
        loadingProgress = 20
    )

    val channels = if (source.url.startsWith("content://")) {
        emptyList()
    } else {
        withContext(Dispatchers.IO) { M3uParser.loadFromUrl(source.url) }
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
        channelsLoaded = true,
        isLoading = false,
        loadingProgress = 100,
        loadingMessage = "✅ ${limited.size} قناة",
        dataVersion = _uiState.value.dataVersion + 1
    )
}

fun loginM3u(url: String, content: String? = null, onDone: (Boolean) -> Unit) {
    viewModelScope.launch {
        _uiState.value = DashboardUiState(
            isLoading = true,
            loadingMessage = "📥 جاري تحميل القائمة...",
            loadingProgress = 20
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
                channelsLoaded = true,
                loadingProgress = 100,
                dataVersion = 1
            )
            onDone(true)
        }
    }
}
