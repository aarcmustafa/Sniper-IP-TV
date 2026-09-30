package com.stitten.stitteniptv.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stitten.stitteniptv.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val loadingProgress: Int = 0,
    val channels: List<Channel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
    val error: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = PrefsManager(app)
    val favorites = FavoritesManager(app)
    val favoritesSync = FavoritesSyncManager(app)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _favoritesVersion = MutableStateFlow(0)
    val favoritesVersion: StateFlow<Int> = _favoritesVersion.asStateFlow()

    init {
        XtreamApi.setPreferredProtocol(prefs.useHttps)
        XtreamApi.setPreferredFormat(prefs.streamFormat)
    }

    /**
     * الحل الجذري: تحميل تدريجي بنسبة واضحة
     */
    fun loginXtream(server: String, user: String, pass: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            XtreamApi.setPreferredProtocol(prefs.useHttps)
            XtreamApi.setPreferredFormat(prefs.streamFormat)

            // ========== المرحلة 1: التحقق (5%) ==========
            _uiState.value = DashboardUiState(
                isLoading = true,
                loadingMessage = "🔐 جاري التحقق من البيانات...",
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

            // ========== المرحلة 2: القنوات (10% → 45%) ==========
            _uiState.value = _uiState.value.copy(
                loadingMessage = "📺 جاري تحميل القنوات المباشرة...",
                loadingProgress = 10
            )

            val live = withContext(Dispatchers.IO) {
                XtreamApi.loadLiveStreams(server, user, pass)
            }
            ContentRepository.channels = live
            _uiState.value = _uiState.value.copy(
                channels = live,
                loadingProgress = 45,
                loadingMessage = "✅ تم تحميل ${live.size} قناة"
            )

            delay(300)

            // ========== المرحلة 3: الأفلام (45% → 75%) ==========
            _uiState.value = _uiState.value.copy(
                loadingMessage = "🎬 جاري تحميل الأفلام...",
                loadingProgress = 50
            )

            val vod = withContext(Dispatchers.IO) {
                XtreamApi.loadVodStreams(server, user, pass)
            }
            ContentRepository.movies = vod
            _uiState.value = _uiState.value.copy(
                movies = vod,
                loadingProgress = 75,
                loadingMessage = "✅ تم تحميل ${vod.size} فيلم"
            )

            delay(300)

            // ========== المرحلة 4: المسلسلات (75% → 100%) ==========
            _uiState.value = _uiState.value.copy(
                loadingMessage = "📼 جاري تحميل المسلسلات...",
                loadingProgress = 80
            )

            val srs = withContext(Dispatchers.IO) {
                XtreamApi.loadSeries(server, user, pass)
            }
            ContentRepository.series = srs

            if (prefs.favoritesSyncEnabled) {
                favoritesSync.syncAll(live, vod, srs, favorites)
                _favoritesVersion.value++
            }

            _uiState.value = DashboardUiState(
                isLoading = false,
                channels = live,
                movies = vod,
                series = srs,
                loadingProgress = 100
            )
        }
    }

    /**
     * تحميل قنوات تصنيف معين (للتحميل التدريجي)
     */
    fun loadCategoryContent(
        categoryId: String,
        contentType: String,
        onDone: (List<Channel>) -> Unit
    ) {
        viewModelScope.launch {
            val channels = withContext(Dispatchers.IO) {
                XtreamApi.loadLiveStreamsByCategory(
                    prefs.serverUrl, prefs.username, prefs.password, categoryId
                )
            }
            ContentRepository.channels = ContentRepository.channels + channels
            _uiState.value = _uiState.value.copy(
                channels = ContentRepository.channels
            )
            onDone(channels)
        }
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
                ContentRepository.channels = list
                if (prefs.favoritesSyncEnabled) {
                    favoritesSync.syncChannels(list, favorites)
                    _favoritesVersion.value++
                }
                _uiState.value = DashboardUiState(
                    channels = list,
                    loadingProgress = 100
                )
                onDone(true)
            }
        }
    }
    
    fun loadCachedContent() {
        viewModelScope.launch {
            XtreamApi.setPreferredProtocol(prefs.useHttps)
            XtreamApi.setPreferredFormat(prefs.streamFormat)

            _uiState.value = DashboardUiState(
                isLoading = true,
                loadingMessage = "🔄 جاري التحميل...",
                loadingProgress = 10
            )

            when (prefs.loginType) {
                "M3U" -> {
                    val list = M3uParser.loadFromUrl(prefs.m3uUrl)
                    ContentRepository.channels = list
                    if (prefs.favoritesSyncEnabled) {
                        favoritesSync.syncChannels(list, favorites)
                        _favoritesVersion.value++
                    }
                    _uiState.value = DashboardUiState(
                        channels = list,
                        loadingProgress = 100
                    )
                }
                "XTREAM" -> {
                    _uiState.value = _uiState.value.copy(
                        loadingMessage = "📺 جاري تحميل القنوات...",
                        loadingProgress = 15
                    )
                    val live = withContext(Dispatchers.IO) {
                        XtreamApi.loadLiveStreams(
                            prefs.serverUrl, prefs.username, prefs.password
                        )
                    }
                    ContentRepository.channels = live
                    _uiState.value = _uiState.value.copy(
                        channels = live,
                        loadingProgress = 45,
                        loadingMessage = "✅ ${live.size} قناة"
                    )

                    delay(300)

                    _uiState.value = _uiState.value.copy(
                        loadingMessage = "🎬 جاري تحميل الأفلام...",
                        loadingProgress = 50
                    )
                    val vod = withContext(Dispatchers.IO) {
                        XtreamApi.loadVodStreams(
                            prefs.serverUrl, prefs.username, prefs.password
                        )
                    }
                    ContentRepository.movies = vod
                    _uiState.value = _uiState.value.copy(
                        movies = vod,
                        loadingProgress = 75,
                        loadingMessage = "✅ ${vod.size} فيلم"
                    )

                    delay(300)

                    _uiState.value = _uiState.value.copy(
                        loadingMessage = "📼 جاري تحميل المسلسلات...",
                        loadingProgress = 80
                    )
                    val srs = withContext(Dispatchers.IO) {
                        XtreamApi.loadSeries(
                            prefs.serverUrl, prefs.username, prefs.password
                        )
                    }
                    ContentRepository.series = srs

                    if (prefs.favoritesSyncEnabled) {
                        favoritesSync.syncAll(live, vod, srs, favorites)
                        _favoritesVersion.value++
                    }

                    _uiState.value = DashboardUiState(
                        isLoading = false,
                        channels = live,
                        movies = vod,
                        series = srs,
                        loadingProgress = 100
                    )
                }
            }
        }
    }

    fun loadEpisodes(seriesId: String, onDone: (List<Episode>) -> Unit) {
        viewModelScope.launch {
            val eps = XtreamApi.loadEpisodes(
                prefs.serverUrl, prefs.username, prefs.password, seriesId
            )
            ContentRepository.episodes = ContentRepository.episodes + (seriesId to eps)
            onDone(eps)
        }
    }

    fun toggleChannelFavorite(id: String, name: String = "") {
        val nowFav = favorites.toggleChannel(id, name)
        if (name.isNotBlank()) {
            if (nowFav) favoritesSync.markChannel(name)
            else favoritesSync.unmarkChannel(name)
        }
        _favoritesVersion.value++
    }

    fun toggleMovieFavorite(id: String, name: String = "") {
        val nowFav = favorites.toggleMovie(id, name)
        if (name.isNotBlank()) {
            if (nowFav) favoritesSync.markMovie(name)
            else favoritesSync.unmarkMovie(name)
        }
        _favoritesVersion.value++
    }

    fun toggleSeriesFavorite(id: String, name: String = "") {
        val nowFav = favorites.toggleSeries(id, name)
        if (name.isNotBlank()) {
            if (nowFav) favoritesSync.markSeries(name)
            else favoritesSync.unmarkSeries(name)
        }
        _favoritesVersion.value++
    }

    fun getFavoriteChannels(): List<Channel> {
        val ids = favorites.getFavoriteChannelIds()
        return ContentRepository.channels.filter { it.id in ids }
    }

    fun getFavoriteMovies(): List<Movie> {
        val ids = favorites.getFavoriteMovieIds()
        return ContentRepository.movies.filter { it.id in ids }
    }

    fun getFavoriteSeries(): List<Series> {
        val ids = favorites.getFavoriteSeriesIds()
        return ContentRepository.series.filter { it.id in ids }
    }

    fun logout() {
        prefs.clear()
        ContentRepository.clear()
        _uiState.value = DashboardUiState()
    }
}
