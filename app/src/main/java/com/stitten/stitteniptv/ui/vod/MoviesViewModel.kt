package com.stitten.stitteniptv.ui.vod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.stitten.stitteniptv.data.ChannelRepository
import com.stitten.stitteniptv.database.entity.MovieEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoviesUiState(
    val isLoading: Boolean = true,
    val totalCount: Int = 0,
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
)

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val repository: ChannelRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    var moviesPaged: Flow<PagingData<MovieEntity>> =
        repository.getLatestMoviesPaged().cachedIn(viewModelScope)

    init {
        loadInfo()
    }

    private fun loadInfo() {
        viewModelScope.launch {
            try {
                val count = repository.getMovieCount()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    totalCount = count,
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }

        viewModelScope.launch {
            repository.getMovieCategories().collectLatest { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
            }
        }
    }

    fun selectCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        moviesPaged = if (category.isNullOrBlank()) {
            repository.getLatestMoviesPaged().cachedIn(viewModelScope)
        } else {
            repository.getMoviesByCategoryPaged(category).cachedIn(viewModelScope)
        }
    }
}
