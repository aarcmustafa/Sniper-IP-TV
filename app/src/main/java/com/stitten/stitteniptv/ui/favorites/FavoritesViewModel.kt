package com.stitten.stitteniptv.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stitten.stitteniptv.data.Channel
import com.stitten.stitteniptv.data.ContentRepository
import com.stitten.stitteniptv.data.FavoritesManager
import com.stitten.stitteniptv.data.Movie
import com.stitten.stitteniptv.data.Series
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FavoritesTab { CHANNELS, MOVIES, SERIES }

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val selectedTab: FavoritesTab = FavoritesTab.MOVIES,
    val channels: List<Channel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {

    private val favorites = FavoritesManager(application)

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val channelIds = favorites.getFavoriteChannelIds()
                val movieIds = favorites.getFavoriteMovieIds()
                val seriesIds = favorites.getFavoriteSeriesIds()

                val channels = ContentRepository.channels.filter { it.id in channelIds }
                val movies = ContentRepository.movies.filter { it.id in movieIds }
                val series = ContentRepository.series.filter { it.id in seriesIds }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    channels = channels,
                    movies = movies,
                    series = series,
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectTab(tab: FavoritesTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun removeChannelFavorite(id: String) {
        favorites.toggleChannel(id, "")
        loadFavorites()
    }

    fun removeMovieFavorite(id: String) {
        favorites.toggleMovie(id, "")
        loadFavorites()
    }

    fun removeSeriesFavorite(id: String) {
        favorites.toggleSeries(id, "")
        loadFavorites()
    }
}
