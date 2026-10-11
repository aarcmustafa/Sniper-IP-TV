package com.stitten.stitteniptv.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.stitten.stitteniptv.data.ChannelRepository
import com.stitten.stitteniptv.database.entity.ChannelEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveUiState(
    val isLoading: Boolean = true,
    val totalCount: Int = 0,
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
)

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val repository: ChannelRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    // القنوات المعروضة حاليًا (All أو حسب تصنيف)
    var channelsPaged: Flow<PagingData<ChannelEntity>> =
        repository.getAllChannelsPaged().cachedIn(viewModelScope)

    init {
        loadInfo()
    }

    private fun loadInfo() {
        viewModelScope.launch {
            try {
                val count = repository.getChannelCount()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    totalCount = count,
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }

        viewModelScope.launch {
            repository.getChannelGroups().collectLatest { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
            }
        }
    }

    fun selectCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        channelsPaged = if (category.isNullOrBlank()) {
            repository.getAllChannelsPaged().cachedIn(viewModelScope)
        } else {
            repository.getChannelsByGroupPaged(category).cachedIn(viewModelScope)
        }
    }
}
