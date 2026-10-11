package com.stitten.stitteniptv.ui.vod

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.stitten.stitteniptv.data.ChannelRepository
import com.stitten.stitteniptv.data.FavoritesManager
import com.stitten.stitteniptv.database.entity.MovieEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MovieDetailTab { OVERVIEW, CAST, SIMILAR }

data class MovieDetailsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val movie: MovieEntity? = null,
    val isFavorite: Boolean = false,
    val selectedTab: MovieDetailTab = MovieDetailTab.OVERVIEW,
    val similarMovies: List<MovieEntity> = emptyList(),
)

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    private val repository: ChannelRepository,
    private val application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val movieId: String = savedStateHandle.get<String>("movieId") ?: ""
    private val favorites = FavoritesManager(application)

    private val _uiState = MutableStateFlow(MovieDetailsUiState())
    val uiState: StateFlow<MovieDetailsUiState> = _uiState.asStateFlow()

    init {
        loadMovie()
    }

    fun loadMovie() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                if (movieId.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "معرّف الفيلم غير موجود",
                    )
                    return@launch
                }

                val movie = repository.getMovieById(movieId)
                if (movie == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "الفيلم غير موجود",
                    )
                    return@launch
                }

                // ─── جلب الأفلام المشابهة ───
                val similar = try {
                    val allRecent = repository.getLatestMovies(limit = 50).first()
                    allRecent
                        .filter {
                            it.id != movie.id &&
                                    it.category == movie.category &&
                                    movie.category.isNotBlank()
                        }
                        .take(12)
                } catch (_: Exception) {
                    emptyList()
                }

                _uiState.value = MovieDetailsUiState(
                    isLoading = false,
                    errorMessage = null,
                    movie = movie,
                    isFavorite = isMovieFavorite(movie.id),
                    selectedTab = MovieDetailTab.OVERVIEW,
                    similarMovies = similar,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "خطأ في التحميل",
                )
            }
        }
    }

    fun retry() = loadMovie()

    fun selectTab(tab: MovieDetailTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun toggleFavorite() {
        val movie = _uiState.value.movie ?: return
        val newState = favorites.toggleMovie(movie.id, movie.name)
        _uiState.value = _uiState.value.copy(isFavorite = newState)
    }

    private fun isMovieFavorite(id: String): Boolean {
        return try {
            favorites.getFavoriteMovieIds().contains(id)
        } catch (_: Exception) {
            false
        }
    }
}
