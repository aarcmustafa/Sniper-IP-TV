package com.stitten.stitteniptv.ui.vod

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stitten.stitteniptv.data.ChannelRepository
import com.stitten.stitteniptv.database.entity.MovieEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieDetailsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val movie: MovieEntity? = null,
)

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    private val repository: ChannelRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val movieId: String = savedStateHandle.get<String>("movieId") ?: ""

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
                } else {
                    _uiState.value = MovieDetailsUiState(
                        isLoading = false,
                        errorMessage = null,
                        movie = movie,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "خطأ في التحميل",
                )
            }
        }
    }

    fun retry() = loadMovie()
}
