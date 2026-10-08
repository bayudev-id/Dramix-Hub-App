package com.dramix.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dramix.app.core.database.dao.WatchHistoryDao
import com.dramix.app.core.database.entity.WatchHistoryEntity
import com.dramix.app.data.source.local.ProviderConfigItem
import com.dramix.app.data.source.local.ProviderPreferences
import com.dramix.app.data.source.local.UserProviderConfig
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
    val errorMessage: String? = null,
    val isCustomizingProviders: Boolean = false
)

class HomeViewModel(
    private val catalogRepository: CatalogRepository,
    private val watchHistoryDao: WatchHistoryDao,
    private val providerPreferences: ProviderPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var rawProviders: List<ProviderModel> = emptyList()

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

            rawProviders = providersResult.getOrDefault(emptyList())
            val effectiveProviders = providerPreferences.applyToProviders(rawProviders)

            val currentSelected = _uiState.value.selectedProviderId
            val defaultProvider = if (effectiveProviders.any { it.id == currentSelected }) {
                effectiveProviders.first { it.id == currentSelected }
            } else {
                effectiveProviders.firstOrNull { it.contentType == "long_drama" }
                    ?: effectiveProviders.firstOrNull { it.contentType == "movie_tv" }
                    ?: effectiveProviders.firstOrNull { it.id == "freereels" }
                    ?: effectiveProviders.firstOrNull()
            }

            _uiState.value = _uiState.value.copy(
                providers = effectiveProviders,
                selectedProviderId = defaultProvider?.id
            )

            if (defaultProvider != null) {
                loadProviderFeed(defaultProvider.id)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun getAllProviderConfigs(): List<ProviderConfigItem> {
        return providerPreferences.getMergedConfigItems(rawProviders)
    }

    fun updateProviderConfigs(configs: List<UserProviderConfig>) {
        providerPreferences.saveConfigs(configs)
        val effectiveProviders = providerPreferences.applyToProviders(rawProviders)

        val currentSelected = _uiState.value.selectedProviderId
        val nextSelectedId = if (effectiveProviders.any { it.id == currentSelected }) {
            currentSelected
        } else {
            effectiveProviders.firstOrNull()?.id
        }

        _uiState.value = _uiState.value.copy(
            providers = effectiveProviders,
            selectedProviderId = nextSelectedId
        )

        if (nextSelectedId != null && nextSelectedId != currentSelected) {
            viewModelScope.launch {
                loadProviderFeed(nextSelectedId)
            }
        }
    }

    fun resetProviderConfigs() {
        providerPreferences.resetToDefault()
        val effectiveProviders = providerPreferences.applyToProviders(rawProviders)
        val nextSelectedId = effectiveProviders.firstOrNull()?.id

        _uiState.value = _uiState.value.copy(
            providers = effectiveProviders,
            selectedProviderId = nextSelectedId
        )

        if (nextSelectedId != null) {
            viewModelScope.launch {
                loadProviderFeed(nextSelectedId)
            }
        }
    }

    fun openProviderCustomizer() {
        _uiState.value = _uiState.value.copy(isCustomizingProviders = true)
    }

    fun closeProviderCustomizer() {
        _uiState.value = _uiState.value.copy(isCustomizingProviders = false)
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
