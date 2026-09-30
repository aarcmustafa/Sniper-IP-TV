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
    val dataVersion: Int = 0
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
        // تطبيق Lite Mode على Paging
        channelRepo.liteMode = prefs.liteModeEnabled
    }

    private fun getChannelLimit(): Int =
        if (prefs.liteModeEnabled) 15000 else Int.MAX_VALUE

    private fun getMovieLimit(): Int =
        if (prefs.liteModeEnabled) 10000 else Int.MAX_VALUE

    private fun getSeriesLimit(): Int =
        if (prefs.liteModeEnabled) 5000 else Int.MAX_VALUE

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
                "XTREAM" -> loadXtreamSource(source)
                "M3U" -> loadM3uSource(source)
            }
        }
    }

    private suspend fun loadXtreamSource(source: SourceEntity) {
        val valid = withContext(Dispatchers.IO) {
            XtreamApi.validate(source.url, source.username, source.password)
        }

        if (!valid) {
            _uiState.value = DashboardUiState(
                error = "❌ فشل الاتصال بـ ${source.name}",
                currentSourceName = source.name,
                currentSourceType = source.type
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            loadingMessage = "📺 جاري تحميل القنوات...",
            loadingProgress = 15
        )

        val live = withContext(Dispatchers.IO) {
            XtreamApi.loadLiveStreams(source.url, source.username, source.password)
        }
        val limitedLive = live.take(getChannelLimit())
        withContext(Dispatchers.IO) { channelRepo.saveChannels(limitedLive) }

        _uiState.value = _uiState.value.copy(
            channelsCount = limitedLive.size,
            loadingProgress = 40,
            loadingMessage = "✅ ${limitedLive.size} قناة",
            dataVersion = _uiState.value.dataVersion + 1
        )

        delay(500)

        _uiState.value = _uiState.value.copy(
            loadingMessage = "🎬 جاري تحميل الأفلام...",
            loadingProgress = 45
        )

        val vod = withContext(Dispatchers.IO) {
            XtreamApi.loadVodStreams(source.url, source.username, source.password)
        }
        val limitedVod = vod.take(getMovieLimit())
        withContext(Dispatchers.IO) { channelRepo.saveMovies(limitedVod) }

        _uiState.value = _uiState.value.copy(
            moviesCount = limitedVod.size,
            loadingProgress = 75,
            loadingMessage = "✅ ${limitedVod.size} فيلم",
            dataVersion = _uiState.value.dataVersion + 1
        )

        delay(500)

        _uiState.value = _uiState.value.copy(
            loadingMessage = "📼 جاري تحميل المسلسلات...",
            loadingProgress = 80
        )

        val srs = withContext(Dispatchers.IO) {
            XtreamApi.loadSeries(source.url, source.username, source.password)
        }
        val limitedSrs = srs.take(getSeriesLimit())
        withContext(Dispatchers.IO) { channelRepo.saveSeries(limitedSrs) }

        _uiState.value = _uiState.value.copy(
            seriesCount = limitedSrs.size,
            isLoading = false,
            loadingProgress = 100,
            dataVersion = _uiState.value.dataVersion + 1
        )
    }
    
private suspend fun loadM3uSource(source: SourceEntity) {
    _uiState.value = _uiState.value.copy(
        loadingMessage = "📥 جاري تحميل M3U...",
        loadingProgress = 30
    )

    val channels = if (source.url.startsWith("content://")) {
        emptyList()
    } else {
        withContext(Dispatchers.IO) {
            M3uParser.loadFromUrl(source.url)
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

        val source = SourceEntity(
            name = "الحساب الحالي",
            type = "XTREAM",
            url = server,
            username = user,
            password = pass,
            isActive = true
        )
        loadXtreamSource(source)
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

            val limited = list.take(getChannelLimit())
            withContext(Dispatchers.IO) {
                channelRepo.clearAll()
                channelRepo.saveChannels(limited)
            }

            _uiState.value = DashboardUiState(
                channelsCount = limited.size,
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
            channelRepo.liteMode = prefs.liteModeEnabled

            val active = withContext(Dispatchers.IO) { sourceMgr.getActive() }

            if (active != null) {
                switchSource(active)
                return@launch
            }

            when (prefs.loginType) {
                "M3U" -> {
                    val list = M3uParser.loadFromUrl(prefs.m3uUrl)
                    val limited = list.take(getChannelLimit())
                    withContext(Dispatchers.IO) {
                        channelRepo.clearAll()
                        channelRepo.saveChannels(limited)
                    }
                    _uiState.value = DashboardUiState(
                        channelsCount = limited.size,
                        loadingProgress = 100,
                        dataVersion = 1
                    )
                }
                "XTREAM" -> {
                    val fake = SourceEntity(
                        name = "الحساب الحالي",
                        type = "XTREAM",
                        url = prefs.serverUrl,
                        username = prefs.username,
                        password = prefs.password,
                        isActive = true
                    )
                    loadXtreamSource(fake)
                }
            }
        }
    }

    fun deleteSource(source: SourceEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { sourceMgr.delete(source) }
            onDone()
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
        viewModelScope.launch {
            withContext(Dispatchers.IO) { channelRepo.clearAll() }
            prefs.clear()
            ContentRepository.clear()
            _uiState.value = DashboardUiState()
        }
    }
}
