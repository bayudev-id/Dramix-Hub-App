package com.dramix.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dramix.app.core.database.dao.WatchHistoryDao
import com.dramix.app.core.database.entity.WatchHistoryEntity
import com.dramix.app.domain.model.ProviderModel
import com.dramix.app.domain.model.VideoItem
import com.dramix.app.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val providers: List<ProviderModel> = emptyList(),
    val selectedProviderId: String? = null,
    val spotlightItem: VideoItem? = null,
    val popularVideos: List<VideoItem> = emptyList(),
    val shortDramaVideos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val catalogRepository: CatalogRepository,
    private val watchHistoryDao: WatchHistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val continueWatchingList: StateFlow<List<WatchHistoryEntity>> =
        watchHistoryDao.getLatestWatchedDramas(limit = 10)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val providersResult = catalogRepository.getProviders()
            if (providersResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = providersResult.exceptionOrNull()?.localizedMessage ?: "Gagal memuat provider"
                )
                return@launch
            }

            val providers = providersResult.getOrDefault(emptyList())
            val defaultProvider = providers.firstOrNull { it.contentType == "long_drama" }
                ?: providers.firstOrNull { it.contentType == "movie_tv" }
                ?: providers.firstOrNull { it.id == "freereels" }
                ?: providers.firstOrNull()

            _uiState.value = _uiState.value.copy(
                providers = providers,
                selectedProviderId = defaultProvider?.id
            )

            if (defaultProvider != null) {
                loadProviderFeed(defaultProvider.id)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectProvider(providerId: String) {
        if (_uiState.value.selectedProviderId == providerId) return
        _uiState.value = _uiState.value.copy(selectedProviderId = providerId)
        viewModelScope.launch {
            loadProviderFeed(providerId)
        }
    }

    private suspend fun loadProviderFeed(providerId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        val categoriesResult = catalogRepository.getCategories(providerId)
        val defaultCategory = categoriesResult.getOrNull()?.firstOrNull()?.id ?: "all"

        val videosResult = catalogRepository.getVideos(providerId, defaultCategory, 1)

        if (videosResult.isSuccess) {
            val items = videosResult.getOrDefault(emptyList())
            val spotlight = items.firstOrNull()
            val popular = if (items.isNotEmpty()) items.drop(1) else emptyList()

            // Also fetch short drama sample if available
            val shortDramaProvider = _uiState.value.providers.firstOrNull { it.contentType == "short_drama" }
            val shortDramas = if (shortDramaProvider != null) {
                val shortCat = catalogRepository.getCategories(shortDramaProvider.id).getOrNull()?.firstOrNull()?.id ?: "all"
                val shortVideosResult = catalogRepository.getVideos(shortDramaProvider.id, shortCat, 1)
                shortVideosResult.getOrDefault(emptyList())
            } else {
                emptyList()
            }

            _uiState.value = _uiState.value.copy(
                spotlightItem = spotlight,
                popularVideos = popular,
                shortDramaVideos = shortDramas,
                isLoading = false,
                errorMessage = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = videosResult.exceptionOrNull()?.localizedMessage ?: "Gagal memuat katalog video"
            )
        }
    }
}
