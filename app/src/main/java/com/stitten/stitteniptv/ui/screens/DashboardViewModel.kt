package com.stitten.stitteniptv.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stitten.stitteniptv.data.ChannelRepository
import com.stitten.stitteniptv.data.PrefsManager
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
    val progress: Float = 0f,
    val progressLabel: String = "",
    val errorMessage: String? = null,
    val featuredMovie: MovieEntity? = null,
    val featuredMovies: List<MovieEntity> = emptyList(),
    val continueWatching: List<WatchHistoryEntity> = emptyList(),
    val latestMovies: List<MovieEntity> = emptyList(),
    val topRatedMovies: List<MovieEntity> = emptyList(),
    val allSeries: List<SeriesEntity> = emptyList(),
    val liveChannels: List<ChannelEntity> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val watchHistoryDao: WatchHistoryDao,
    application: Application,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val prefs = PrefsManager(application)
    private val liteMode: Boolean get() = prefs.liteModeEnabled

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                progress = 0f,
                progressLabel = "تحضير المحتوى...",
                errorMessage = null,
            )

            try {
                val movieLimit = if (liteMode) 12 else 30
                val seriesLimit = if (liteMode) 12 else 30
                val channelsLimit = if (liteMode) 15 else 30
                val historyLimit = if (liteMode) 10 else 20

                setProgress(0.15f, "تحميل سجل المشاهدة...")
                val history = watchHistoryDao.getRecent(limit = historyLimit).first()

                setProgress(0.35f, "تحميل الأفلام...")
                val latestMovies = channelRepository.getLatestMovies(limit = movieLimit).first()

                setProgress(0.55f, "تحميل المسلسلات...")
                val seriesList = channelRepository.getFirstSeriesPage(limit = seriesLimit)

                setProgress(0.75f, "تحميل القنوات...")
                val channelsList = channelRepository.getFirstChannelsPage(limit = channelsLimit)

                val topRated = if (!liteMode) {
                    setProgress(0.90f, "تحميل الأعلى تقييمًا...")
                    channelRepository.getTopRatedMovies(limit = 20).first()
                } else {
                    emptyList()
                }

                setProgress(1f, "جاهز")

                val featuredList = buildList {
                    addAll(latestMovies.take(3))
                    topRated.forEach { m ->
                        if (size < 5 && none { it.id == m.id }) add(m)
                    }
                }

                _uiState.value = DashboardUiState(
                    isLoading = false,
                    progress = 1f,
                    progressLabel = "",
                    errorMessage = null,
                    featuredMovie = featuredList.firstOrNull(),
                    featuredMovies = featuredList,
                    continueWatching = history,
                    latestMovies = latestMovies,
                    topRatedMovies = topRated,
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

    private fun setProgress(value: Float, label: String) {
        _uiState.value = _uiState.value.copy(progress = value, progressLabel = label)
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
