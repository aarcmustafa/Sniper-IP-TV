package com.stitten.stitteniptv.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stitten.stitteniptv.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val channels: List<Channel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
    val error: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = PrefsManager(app)
    val favorites = FavoritesManager(app)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _favoritesVersion = MutableStateFlow(0)
    val favoritesVersion: StateFlow<Int> = _favoritesVersion.asStateFlow()

    fun loginM3u(url: String, content: String? = null, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = DashboardUiState(isLoading = true)
            val list = if (content != null) {
                M3uParser.loadFromContent(content)
            } else {
                M3uParser.loadFromUrl(url)
            }
            if (list.isEmpty()) {
                _uiState.value = DashboardUiState(error = "لا توجد قنوات في الملف")
                onDone(false)
            } else {
                prefs.isLoggedIn = true
                prefs.loginType = "M3U"
                prefs.m3uUrl = url
                ContentRepository.channels = list
                _uiState.value = DashboardUiState(channels = list)
                onDone(true)
            }
        }
    }

    fun loginXtream(server: String, user: String, pass: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = DashboardUiState(isLoading = true)
            val valid = XtreamApi.validate(server, user, pass)
            if (!valid) {
                _uiState.value = DashboardUiState(error = "بيانات الدخول غير صحيحة")
                onDone(false)
                return@launch
            }
            prefs.isLoggedIn = true
            prefs.loginType = "XTREAM"
            prefs.serverUrl = server
            prefs.username = user
            prefs.password = pass

            val live = XtreamApi.loadLiveStreams(server, user, pass)
            val vod = XtreamApi.loadVodStreams(server, user, pass)
            val srs = XtreamApi.loadSeries(server, user, pass)
            ContentRepository.channels = live
            ContentRepository.movies = vod
            ContentRepository.series = srs
            _uiState.value = DashboardUiState(channels = live, movies = vod, series = srs)
            onDone(true)
        }
    }

    fun loadCachedContent() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState(isLoading = true)
            when (prefs.loginType) {
                "M3U" -> {
                    val list = M3uParser.loadFromUrl(prefs.m3uUrl)
                    ContentRepository.channels = list
                    _uiState.value = DashboardUiState(channels = list)
                }
                "XTREAM" -> {
                    val live = XtreamApi.loadLiveStreams(prefs.serverUrl, prefs.username, prefs.password)
                    val vod = XtreamApi.loadVodStreams(prefs.serverUrl, prefs.username, prefs.password)
                    val srs = XtreamApi.loadSeries(prefs.serverUrl, prefs.username, prefs.password)
                    ContentRepository.channels = live
                    ContentRepository.movies = vod
                    ContentRepository.series = srs
                    _uiState.value = DashboardUiState(channels = live, movies = vod, series = srs)
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

    fun toggleChannelFavorite(id: String) {
        favorites.toggleChannel(id)
        _favoritesVersion.value++
    }

    fun toggleMovieFavorite(id: String) {
        favorites.toggleMovie(id)
        _favoritesVersion.value++
    }

    fun toggleSeriesFavorite(id: String) {
        favorites.toggleSeries(id)
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
