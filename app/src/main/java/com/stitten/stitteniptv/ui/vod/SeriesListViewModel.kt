package com.stitten.stitteniptv.ui.vod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.stitten.stitteniptv.data.ChannelRepository
import com.stitten.stitteniptv.database.entity.SeriesEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeriesListUiState(
    val isLoading: Boolean = true,
    val totalCount: Int = 0,
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
)

@HiltViewModel
class SeriesListViewModel @Inject constructor(
    private val repository: ChannelRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesListUiState())
    val uiState: StateFlow<SeriesListUiState> = _uiState.asStateFlow()

    var seriesPaged: Flow<PagingData<SeriesEntity>> =
        repository.getAllSeriesPaged().cachedIn(viewModelScope)

    init {
        loadInfo()
    }

    private fun loadInfo() {
        viewModelScope.launch {
            try {
                val count = repository.getSeriesCount()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    totalCount = count,
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }

        viewModelScope.launch {
            repository.getSeriesCategories().collectLatest { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
            }
        }
    }

    fun selectCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        seriesPaged = if (category.isNullOrBlank()) {
            repository.getAllSeriesPaged().cachedIn(viewModelScope)
        } else {
            repository.getSeriesByCategoryPaged(category).cachedIn(viewModelScope)
        }
    }
}
