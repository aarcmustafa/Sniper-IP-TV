package com.stitten.stitteniptv.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stitten.stitteniptv.data.*
import com.stitten.stitteniptv.database.entity.SourceEntity
import kotlinx.coroutines.Dispatchers
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

            loadAllContent(server, user, pass)
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
                loadingMessage = "📥 جاري تحليل قائمة M3U...",
                loadingProgress = 10
            )

            val result: M3uParseResult = if (content != null) {
                M3uParser.loadFromContentStructured(content)
            } else {
                M3uParser.loadFromUrlStructured(url)
            }

            val totalItems = result.channels.size + result.movies.size + result.series.size

            if (totalItems == 0) {
                _uiState.value = DashboardUiState(error = "❌ لا يوجد محتوى في القائمة")
                onDone(false)
                return@launch
            }

            prefs.isLoggedIn = true
            prefs.loginType = "M3U"
            prefs.m3uUrl = url

            _uiState.value = _uiState.value.copy(
                loadingMessage = "💾 جاري حفظ ${result.channels.size} قناة...",
                loadingProgress = 40
            )

            val limitedChannels = result.channels.take(getChannelLimit())
            withContext(Dispatchers.IO) {
                channelRepo.clearAll()
                if (limitedChannels.isNotEmpty()) channelRepo.saveChannels(limitedChannels)
                if (result.movies.isNotEmpty()) channelRepo.saveMovies(result.movies)
                if (result.series.isNotEmpty()) channelRepo.saveSeries(result.series)
            }

            _uiState.value = _uiState.value.copy(
                loadingMessage = "📝 جاري تحديث الذاكرة...",
                loadingProgress = 80
            )

            ContentRepository.channels = limitedChannels
            ContentRepository.movies = result.movies
            ContentRepository.series = result.series
            ContentRepository.episodes = ContentRepository.episodes + result.seriesEpisodes

            // ✅ حفظ الحلقات في Room
            withContext(Dispatchers.IO) {
                result.seriesEpisodes.forEach { (seriesId, eps) ->
                    if (eps.isNotEmpty()) {
                        channelRepo.saveEpisodes(seriesId, eps)
                    }
                }
            }

            _uiState.value = DashboardUiState(
                channelsCount = limitedChannels.size,
                moviesCount = result.movies.size,
                seriesCount = result.series.size,
                loadingProgress = 100,
                loadingMessage = "✅ ${limitedChannels.size} قناة · ${result.movies.size} فيلم · ${result.series.size} مسلسل",
                dataVersion = _uiState.value.dataVersion + 1
            )
            onDone(true)
        }
    }

    // ==========================================
    //  تحميل كل المحتوى: Xtream
    // ==========================================
    private suspend fun loadAllContent(
        server: String,
        user: String,
        pass: String
    ) {
        // ─── 1) القنوات المباشرة ───
        _uiState.value = _uiState.value.copy(
            loadingMessage = "📺 جاري تحميل القنوات...",
            loadingProgress = 5
        )

        val live = withContext(Dispatchers.IO) {
            XtreamApi.loadLiveStreams(server, user, pass) { loaded ->
                val progress = (loaded / 500).coerceAtMost(15)
                _uiState.value = _uiState.value.copy(
                    loadingMessage = "📺 تحميل القنوات... ($loaded)",
                    loadingProgress = 5 + progress
                )
            }
        }
        val liveLimited = live.take(getChannelLimit())
        withContext(Dispatchers.IO) { channelRepo.saveChannels(liveLimited) }
        ContentRepository.channels = liveLimited

        // ─── 2) الأفلام ───
        _uiState.value = _uiState.value.copy(
            loadingMessage = "🎬 جاري تحميل الأفلام...",
            loadingProgress = 25
        )

        val movies = withContext(Dispatchers.IO) {
            XtreamApi.loadVodStreams(server, user, pass) { loaded ->
                val progress = (loaded / 500).coerceAtMost(20)
                _uiState.value = _uiState.value.copy(
                    loadingMessage = "🎬 تحميل الأفلام... ($loaded)",
                    loadingProgress = 25 + progress
                )
            }
        }
        withContext(Dispatchers.IO) { channelRepo.saveMovies(movies) }
        ContentRepository.movies = movies

        // ─── 3) المسلسلات ───
        _uiState.value = _uiState.value.copy(
            loadingMessage = "📺 جاري تحميل المسلسلات...",
            loadingProgress = 50
        )

        val series = withContext(Dispatchers.IO) {
            XtreamApi.loadSeries(server, user, pass) { loaded ->
                val progress = (loaded / 200).coerceAtMost(40)
                _uiState.value = _uiState.value.copy(
                    loadingMessage = "📺 تحميل المسلسلات... ($loaded)",
                    loadingProgress = 50 + progress
                )
            }
        }
        withContext(Dispatchers.IO) { channelRepo.saveSeries(series) }
        ContentRepository.series = series

        // ─── 4) انتهى ───
        _uiState.value = _uiState.value.copy(
            channelsCount = liveLimited.size,
            moviesCount = movies.size,
            seriesCount = series.size,
            isLoading = false,
            loadingProgress = 100,
            loadingMessage = "✅ ${liveLimited.size} قناة · ${movies.size} فيلم · ${series.size} مسلسل",
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
                    loadAllContent(source.url, source.username, source.password)
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

        val result: M3uParseResult = if (source.url.startsWith("content://")) {
            M3uParseResult(
                channels = ContentRepository.channels,
                movies = ContentRepository.movies,
                series = ContentRepository.series,
            )
        } else {
            withContext(Dispatchers.IO) {
                M3uParser.loadFromUrlStreamingStructured(
                    ctx = getApplication(),
                    url = source.url,
                    onProgress = { downloaded, total ->
                        val progress = if (total > 0) {
                            ((downloaded * 100) / total).toInt().coerceIn(5, 60)
                        } else {
                            (downloaded / 2).toInt().coerceIn(5, 60)
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

        val totalItems = result.channels.size + result.movies.size + result.series.size

        if (totalItems == 0) {
            _uiState.value = DashboardUiState(
                error = "❌ فشل تحميل M3U: ${source.name}",
                currentSourceName = source.name,
                currentSourceType = source.type
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            loadingMessage = "💾 جاري حفظ المحتوى...",
            loadingProgress = 70
        )

        val limitedChannels = result.channels.take(getChannelLimit())
        withContext(Dispatchers.IO) {
            if (limitedChannels.isNotEmpty()) channelRepo.saveChannels(limitedChannels)
            if (result.movies.isNotEmpty()) channelRepo.saveMovies(result.movies)
            if (result.series.isNotEmpty()) channelRepo.saveSeries(result.series)
        }

        ContentRepository.channels = limitedChannels
        ContentRepository.movies = result.movies
        ContentRepository.series = result.series
        ContentRepository.episodes = ContentRepository.episodes + result.seriesEpisodes

        // ✅ حفظ الحلقات في Room
        withContext(Dispatchers.IO) {
            result.seriesEpisodes.forEach { (seriesId, eps) ->
                if (eps.isNotEmpty()) {
                    channelRepo.saveEpisodes(seriesId, eps)
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            channelsCount = limitedChannels.size,
            moviesCount = result.movies.size,
            seriesCount = result.series.size,
            isLoading = false,
            loadingProgress = 100,
            loadingMessage = "✅ ${limitedChannels.size} قناة · ${result.movies.size} فيلم · ${result.series.size} مسلسل",
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

                    val result = M3uParser.loadFromUrlStructured(url)

                    val limitedChannels = result.channels.take(getChannelLimit())
                    withContext(Dispatchers.IO) {
                        if (limitedChannels.isNotEmpty()) channelRepo.saveChannels(limitedChannels)
                        if (result.movies.isNotEmpty()) channelRepo.saveMovies(result.movies)
                        if (result.series.isNotEmpty()) channelRepo.saveSeries(result.series)
                    }

                    ContentRepository.channels = limitedChannels
                    ContentRepository.movies = result.movies
                    ContentRepository.series = result.series
                    ContentRepository.episodes = ContentRepository.episodes + result.seriesEpisodes

                    // ✅ حفظ الحلقات
                    withContext(Dispatchers.IO) {
                        result.seriesEpisodes.forEach { (seriesId, eps) ->
                            if (eps.isNotEmpty()) channelRepo.saveEpisodes(seriesId, eps)
                        }
                    }

                    _uiState.value = DashboardUiState(
                        channelsCount = limitedChannels.size,
                        moviesCount = result.movies.size,
                        seriesCount = result.series.size,
                        loadingProgress = 100
                    )
                }
                "XTREAM" -> {
                    loadAllContent(
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

                    val result = M3uParser.loadFromUrlStructured(prefs.m3uUrl)

                    val limitedChannels = result.channels.take(getChannelLimit())
                    withContext(Dispatchers.IO) {
                        channelRepo.clearAll()
                        if (limitedChannels.isNotEmpty()) channelRepo.saveChannels(limitedChannels)
                        if (result.movies.isNotEmpty()) channelRepo.saveMovies(result.movies)
                        if (result.series.isNotEmpty()) channelRepo.saveSeries(result.series)
                    }

                    ContentRepository.channels = limitedChannels
                    ContentRepository.movies = result.movies
                    ContentRepository.series = result.series
                    ContentRepository.episodes = ContentRepository.episodes + result.seriesEpisodes

                    // ✅ حفظ الحلقات
                    withContext(Dispatchers.IO) {
                        result.seriesEpisodes.forEach { (seriesId, eps) ->
                            if (eps.isNotEmpty()) channelRepo.saveEpisodes(seriesId, eps)
                        }
                    }

                    _uiState.value = DashboardUiState(
                        channelsCount = limitedChannels.size,
                        moviesCount = result.movies.size,
                        seriesCount = result.series.size,
                        loadingProgress = 100
                    )
                }
                "XTREAM" -> {
                    if (prefs.serverUrl.isBlank()) return@launch
                    loadAllContent(
                        prefs.serverUrl,
                        prefs.username,
                        prefs.password
                    )
                }
            }
        }
    }

    // ==========================================
    //  حذف مصدر
    // ==========================================
    fun deleteSource(source: SourceEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { sourceMgr.delete(source) }
            onDone()
        }
    }

    // ==========================================
    //  ✅ تحميل حلقات مسلسل — Room + Xtream
    // ==========================================
    fun loadEpisodes(seriesId: String, onDone: (List<Episode>) -> Unit) {
        viewModelScope.launch {
            // ─── 1) من الذاكرة أولًا ───
            val cached = ContentRepository.episodes[seriesId]
            if (!cached.isNullOrEmpty()) {
                onDone(cached)
                return@launch
            }

            // ─── 2) من Room ───
            try {
                val fromDb = withContext(Dispatchers.IO) {
                    channelRepo.getEpisodesBySeries(seriesId)
                }
                if (fromDb.isNotEmpty()) {
                    ContentRepository.episodes =
                        ContentRepository.episodes + (seriesId to fromDb)
                    onDone(fromDb)
                    return@launch
                }
            } catch (_: Exception) { }

            // ─── 3) من Xtream (فقط إذا كان المصدر Xtream) ───
            if (prefs.loginType != "XTREAM") {
                onDone(emptyList())
                return@launch
            }

            try {
                val eps = XtreamApi.loadEpisodes(
                    prefs.serverUrl,
                    prefs.username,
                    prefs.password,
                    seriesId
                )
                ContentRepository.episodes =
                    ContentRepository.episodes + (seriesId to eps)

                // ✅ حفظ في Room
                if (eps.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        channelRepo.saveEpisodes(seriesId, eps)
                    }
                }

                onDone(eps)
            } catch (_: Exception) {
                onDone(emptyList())
            }
        }
    }

    // ==========================================
    //  المفضلة - قنوات
    // ==========================================
    fun toggleChannelFavorite(id: String, name: String = "") {
        val nowFav = favorites.toggleChannel(id, name)
        if (name.isNotBlank()) {
            if (nowFav) favoritesSync.markChannel(name)
            else favoritesSync.unmarkChannel(name)
        }
        _favoritesVersion.value++
    }

    fun getFavoriteChannels(): List<Channel> {
        val ids = favorites.getFavoriteChannelIds()
        return ContentRepository.channels.filter { it.id in ids }
    }

    // ==========================================
    //  المفضلة - أفلام
    // ==========================================
    fun toggleMovieFavorite(id: String, name: String = "") {
        val nowFav = favorites.toggleMovie(id, name)
        if (name.isNotBlank()) {
            if (nowFav) favoritesSync.markMovie(name)
            else favoritesSync.unmarkMovie(name)
        }
        _favoritesVersion.value++
    }

    fun getFavoriteMovies(): List<Movie> {
        val ids = favorites.getFavoriteMovieIds()
        return ContentRepository.movies.filter { it.id in ids }
    }

    // ==========================================
    //  المفضلة - مسلسلات
    // ==========================================
    fun toggleSeriesFavorite(id: String, name: String = "") {
        val nowFav = favorites.toggleSeries(id, name)
        if (name.isNotBlank()) {
            if (nowFav) favoritesSync.markSeries(name)
            else favoritesSync.unmarkSeries(name)
        }
        _favoritesVersion.value++
    }

    fun getFavoriteSeries(): List<Series> {
        val ids = favorites.getFavoriteSeriesIds()
        return ContentRepository.series.filter { it.id in ids }
    }

    // ==========================================
    //  سجل المشاهدة (متابعة المشاهدة)
    // ==========================================
    fun removeFromHistory(contentId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                historyMgr.delete(contentId)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                historyMgr.clearAll()
            }
        }
    }

    // ==========================================
    //  تسجيل الخروج
    // ==========================================
    fun logout() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { channelRepo.clearAll() }
            prefs.clear()
            ContentRepository.clear()
            _uiState.value = DashboardUiState()
        }
    }

    // ==========================================
    //  مسح الأخطاء
    // ==========================================
    fun clearErrors() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { errorLogger.clear() }
        }
    }
}
