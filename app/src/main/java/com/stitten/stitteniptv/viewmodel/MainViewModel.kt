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
import dagger.hilt.android.EntryPointAccessors

data class DashboardUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val loadingProgress: Int = 0,
    val channelsCount: Int = 0,
    val moviesCount: Int = 0,
    val seriesCount: Int = 0,
    val error: String? = null,
    val dataVersion: Int = 0
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = PrefsManager(app)
    val favorites = FavoritesManager(app)
    val favoritesSync = FavoritesSyncManager(app)

    // الوصول إلى ChannelRepository عبر Hilt EntryPoint
    private val entryPoint = EntryPointAccessors.fromApplication(
        app.applicationContext,
        com.stitten.stitteniptv.ui.screens.PlayerEntryPoint::class.java
    )
    val channelRepo = entryPoint.channelRepository()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _favoritesVersion = MutableStateFlow(0)
    val favoritesVersion: StateFlow<Int> = _favoritesVersion.asStateFlow()

    init {
        XtreamApi.setPreferredProtocol(prefs.useHttps)
        XtreamApi.setPreferredFormat(prefs.streamFormat)
    }

    /**
     * الحل الجذري: تسجيل دخول + حفظ في Room
     */
    fun loginXtream(server: String, user: String, pass: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            XtreamApi.setPreferredProtocol(prefs.useHttps)
            XtreamApi.setPreferredFormat(prefs.streamFormat)

            // ========== التحقق ==========
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

            // ========== القنوات (10% → 45%) ==========
            _uiState.value = _uiState.value.copy(
                loadingMessage = "📺 جاري تحميل القنوات...",
                loadingProgress = 15
            )

            // حذف القنوات القديمة
            withContext(Dispatchers.IO) {
                channelRepo.clearAll()
            }

            val live = withContext(Dispatchers.IO) {
                XtreamApi.loadLiveStreams(server, user, pass)
            }

            // حفظ في Room
            withContext(Dispatchers.IO) {
                channelRepo.saveChannels(live)
            }

            _uiState.value = _uiState.value.copy(
                channelsCount = live.size,
                loadingProgress = 45,
                loadingMessage = "✅ ${live.size} قناة",
                dataVersion = _uiState.value.dataVersion + 1
            )

            delay(300)

            // ========== الأفلام (50% → 75%) ==========
            _uiState.value = _uiState.value.copy(
                loadingMessage = "🎬 جاري تحميل الأفلام...",
                loadingProgress = 50
            )

            val vod = withContext(Dispatchers.IO) {
                XtreamApi.loadVodStreams(server, user, pass)
            }

            withContext(Dispatchers.IO) {
                channelRepo.saveMovies(vod)
            }

            _uiState.value = _uiState.value.copy(
                moviesCount = vod.size,
                loadingProgress = 75,
                loadingMessage = "✅ ${vod.size} فيلم",
                dataVersion = _uiState.value.dataVersion + 1
            )

            delay(300)

            // ========== المسلسلات (80% → 100%) ==========
            _uiState.value = _uiState.value.copy(
                loadingMessage = "📼 جاري تحميل المسلسلات...",
                loadingProgress = 80
            )

            val srs = withContext(Dispatchers.IO) {
                XtreamApi.loadSeries(server, user, pass)
            }

            withContext(Dispatchers.IO) {
                channelRepo.saveSeries(srs)
            }

            _uiState.value = _uiState.value.copy(
                seriesCount = srs.size,
                isLoading = false,
                loadingProgress = 100,
                dataVersion = _uiState.value.dataVersion + 1
            )
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

            // حذف القديم + الحفظ في Room
            withContext(Dispatchers.IO) {
                channelRepo.clearAll()
                channelRepo.saveChannels(list)
            }

            _uiState.value = DashboardUiState(
                channelsCount = list.size,
                loadingProgress = 100,
                dataVersion = 1
            )
            onDone(true)
        }
    }
}

fun loadCachedContent() {
    viewModelScope.launch {
        XtreamApi.setPreferredProtocol(prefs.useHttps)
        XtreamApi.setPreferredFormat(prefs.streamFormat)

        when (prefs.loginType) {
            "M3U" -> {
                _uiState.value = _uiState.value.copy(
                    loadingMessage = "📥 جاري التحميل...",
                    loadingProgress = 50
                )
                val list = M3uParser.loadFromUrl(prefs.m3uUrl)
                withContext(Dispatchers.IO) {
                    channelRepo.clearAll()
                    channelRepo.saveChannels(list)
                }
                _uiState.value = DashboardUiState(
                    channelsCount = list.size,
                    loadingProgress = 100,
                    dataVersion = 1
                )
            }
            "XTREAM" -> {
                _uiState.value = _uiState.value.copy(
                    loadingMessage = "📺 جاري التحميل...",
                    loadingProgress = 15
                )
                val live = withContext(Dispatchers.IO) {
                    XtreamApi.loadLiveStreams(
                        prefs.serverUrl, prefs.username, prefs.password
                    )
                }
                withContext(Dispatchers.IO) {
                    channelRepo.clearAll()
                    channelRepo.saveChannels(live)
                }
                _uiState.value = _uiState.value.copy(
                    channelsCount = live.size,
                    loadingProgress = 45,
                    loadingMessage = "✅ ${live.size} قناة",
                    dataVersion = _uiState.value.dataVersion + 1
                )

                delay(300)
                val vod = withContext(Dispatchers.IO) {
                    XtreamApi.loadVodStreams(
                        prefs.serverUrl, prefs.username, prefs.password
                    )
                }
                withContext(Dispatchers.IO) {
                    channelRepo.saveMovies(vod)
                }
                _uiState.value = _uiState.value.copy(
                    moviesCount = vod.size,
                    loadingProgress = 75,
                    loadingMessage = "✅ ${vod.size} فيلم",
                    dataVersion = _uiState.value.dataVersion + 1
                )

                delay(300)
                val srs = withContext(Dispatchers.IO) {
                    XtreamApi.loadSeries(
                        prefs.serverUrl, prefs.username, prefs.password
                    )
                }
                withContext(Dispatchers.IO) {
                    channelRepo.saveSeries(srs)
                }
                _uiState.value = _uiState.value.copy(
                    seriesCount = srs.size,
                    isLoading = false,
                    loadingProgress = 100,
                    dataVersion = _uiState.value.dataVersion + 1
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

    // ============== المفضلة ==============
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
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                channelRepo.clearAll()
            }
            prefs.clear()
            ContentRepository.clear()
            _uiState.value = DashboardUiState()
        }
    }
}
