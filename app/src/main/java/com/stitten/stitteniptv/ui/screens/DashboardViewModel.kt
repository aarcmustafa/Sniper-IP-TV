package com.stitten.stitteniptv.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stitten.stitteniptv.data.ChannelRepository
import com.stitten.stitteniptv.database.dao.WatchHistoryDao
import com.stitten.stitteniptv.database.entity.ChannelEntity
import com.stitten.stitteniptv.database.entity.MovieEntity
import com.stitten.stitteniptv.database.entity.SeriesEntity
import com.stitten.stitteniptv.database.entity.WatchHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val featuredMovie: MovieEntity? = null,
    val continueWatching: List<WatchHistoryEntity> = emptyList(),
    val latestMovies: List<MovieEntity> = emptyList(),
    val allSeries: List<SeriesEntity> = emptyList(),
    val liveChannels: List<ChannelEntity> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val watchHistoryDao: WatchHistoryDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // جلب متوازٍ للبيانات
                val latestMovies = channelRepository.getLatestMovies(limit = 30).first()
                val topRated = channelRepository.getTopRatedMovies(limit = 10).first()
                val history = watchHistoryDao.getRecent(limit = 20).first()
                val seriesList = channelRepository.getFirstSeriesPage(limit = 30)
                val channelsList = channelRepository.getFirstChannelsPage(limit = 30)

                _uiState.value = DashboardUiState(
                    isLoading = false,
                    errorMessage = null,
                    featuredMovie = latestMovies.firstOrNull() ?: topRated.firstOrNull(),
                    continueWatching = history,
                    latestMovies = latestMovies,
                    allSeries = seriesList,
                    liveChannels = channelsList,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "تعذّر تحميل المحتوى",
                )
            }
        }
    }

    fun retry() = loadDashboard()

    fun removeFromHistory(item: WatchHistoryEntity) {
        viewModelScope.launch {
            try {
                watchHistoryDao.delete(item.contentId)
                _uiState.value = _uiState.value.copy(
                    continueWatching = _uiState.value.continueWatching.filterNot {
                        it.contentId == item.contentId
                    },
                )
            } catch (_: Exception) { }
        }
    }
}
