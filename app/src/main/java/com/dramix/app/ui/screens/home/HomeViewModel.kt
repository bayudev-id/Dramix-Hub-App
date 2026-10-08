package com.dramix.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dramix.app.core.database.dao.WatchHistoryDao
import com.dramix.app.core.database.entity.WatchHistoryEntity
import com.dramix.app.data.source.local.ProviderConfigItem
import com.dramix.app.data.source.local.ProviderPreferences
import com.dramix.app.data.source.local.UserProviderConfig
import com.dramix.app.domain.model.Category
import com.dramix.app.domain.model.ProviderModel
import com.dramix.app.domain.model.VideoItem
import com.dramix.app.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContentTypeOption(
    val id: String,
    val label: String,
    val count: Int
)

data class HomeUiState(
    val contentTypes: List<ContentTypeOption> = emptyList(),
    val selectedContentType: String? = null,
    val providers: List<ProviderModel> = emptyList(),
    val filteredProviders: List<ProviderModel> = emptyList(),
    val selectedProviderId: String? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val categoryVideos: List<VideoItem> = emptyList(),
    val spotlightItem: VideoItem? = null,
    val popularVideos: List<VideoItem> = emptyList(),
    val shortDramaVideos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingCategories: Boolean = false,
    val isLoadingContent: Boolean = false,
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
            val contentTypes = buildContentTypes(effectiveProviders)

            val currentSelected = _uiState.value.selectedProviderId
            val defaultProvider = if (effectiveProviders.any { it.id == currentSelected }) {
                effectiveProviders.first { it.id == currentSelected }
            } else {
                effectiveProviders.firstOrNull()
            }

            val selectedContentType = defaultProvider?.contentType
                ?: contentTypes.firstOrNull()?.id

            val filteredProviders = if (selectedContentType != null) {
                effectiveProviders.filter { it.contentType == selectedContentType }
            } else {
                effectiveProviders
            }

            _uiState.value = _uiState.value.copy(
                contentTypes = contentTypes,
                selectedContentType = selectedContentType,
                providers = effectiveProviders,
                filteredProviders = filteredProviders,
                selectedProviderId = defaultProvider?.id
            )

            if (defaultProvider != null) {
                loadProviderCategoriesAndFeed(defaultProvider.id, categoryId = null)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectContentType(contentType: String) {
        if (_uiState.value.selectedContentType == contentType) return
        val effectiveProviders = providerPreferences.applyToProviders(rawProviders)
        val filtered = effectiveProviders.filter { it.contentType == contentType }
        val newSelectedProvider = filtered.firstOrNull()?.id

        _uiState.value = _uiState.value.copy(
            selectedContentType = contentType,
            filteredProviders = filtered,
            selectedProviderId = newSelectedProvider,
            categories = emptyList(),
            selectedCategoryId = null,
            categoryVideos = emptyList(),
            spotlightItem = null,
            popularVideos = emptyList(),
            isLoadingContent = true
        )

        if (newSelectedProvider != null) {
            viewModelScope.launch {
                loadProviderCategoriesAndFeed(newSelectedProvider, categoryId = null)
            }
        } else {
            _uiState.value = _uiState.value.copy(isLoadingContent = false)
        }
    }

    fun selectProvider(providerId: String) {
        if (_uiState.value.selectedProviderId == providerId) return
        val prov = _uiState.value.providers.firstOrNull { it.id == providerId }
        val contentType = prov?.contentType ?: _uiState.value.selectedContentType

        val filtered = if (contentType != null) {
            _uiState.value.providers.filter { it.contentType == contentType }
        } else {
            _uiState.value.filteredProviders
        }

        _uiState.value = _uiState.value.copy(
            selectedContentType = contentType,
            filteredProviders = filtered,
            selectedProviderId = providerId,
            categories = emptyList(),
            selectedCategoryId = null,
            categoryVideos = emptyList(),
            spotlightItem = null,
            popularVideos = emptyList(),
            isLoadingContent = true
        )

        viewModelScope.launch {
            loadProviderCategoriesAndFeed(providerId, categoryId = null)
        }
    }

    fun selectCategory(categoryId: String) {
        if (_uiState.value.selectedCategoryId == categoryId) return
        val providerId = _uiState.value.selectedProviderId ?: return

        _uiState.value = _uiState.value.copy(
            selectedCategoryId = categoryId,
            isLoadingContent = true
        )

        viewModelScope.launch {
            loadCategoryVideos(providerId, categoryId)
        }
    }

    fun getAllProviderConfigs(): List<ProviderConfigItem> {
        return providerPreferences.getMergedConfigItems(rawProviders)
    }

    fun updateProviderConfigs(configs: List<UserProviderConfig>) {
        providerPreferences.saveConfigs(configs)
        val effectiveProviders = providerPreferences.applyToProviders(rawProviders)
        val contentTypes = buildContentTypes(effectiveProviders)

        val currentType = _uiState.value.selectedContentType
        val nextType = if (contentTypes.any { it.id == currentType }) {
            currentType
        } else {
            contentTypes.firstOrNull()?.id
        }

        val filtered = if (nextType != null) {
            effectiveProviders.filter { it.contentType == nextType }
        } else {
            effectiveProviders
        }

        val currentProv = _uiState.value.selectedProviderId
        val nextProv = if (filtered.any { it.id == currentProv }) {
            currentProv
        } else {
            filtered.firstOrNull()?.id
        }

        _uiState.value = _uiState.value.copy(
            providers = effectiveProviders,
            contentTypes = contentTypes,
            selectedContentType = nextType,
            filteredProviders = filtered,
            selectedProviderId = nextProv
        )

        if (nextProv != null && nextProv != currentProv) {
            viewModelScope.launch {
                loadProviderCategoriesAndFeed(nextProv, categoryId = null)
            }
        }
    }

    fun resetProviderConfigs() {
        providerPreferences.resetToDefault()
        val effectiveProviders = providerPreferences.applyToProviders(rawProviders)
        val contentTypes = buildContentTypes(effectiveProviders)

        val defaultProvider = effectiveProviders.firstOrNull()
        val selectedContentType = defaultProvider?.contentType ?: contentTypes.firstOrNull()?.id

        val filtered = if (selectedContentType != null) {
            effectiveProviders.filter { it.contentType == selectedContentType }
        } else {
            effectiveProviders
        }

        val nextSelectedId = defaultProvider?.id

        _uiState.value = _uiState.value.copy(
            providers = effectiveProviders,
            contentTypes = contentTypes,
            selectedContentType = selectedContentType,
            filteredProviders = filtered,
            selectedProviderId = nextSelectedId
        )

        if (nextSelectedId != null) {
            viewModelScope.launch {
                loadProviderCategoriesAndFeed(nextSelectedId, categoryId = null)
            }
        }
    }

    fun openProviderCustomizer() {
        _uiState.value = _uiState.value.copy(isCustomizingProviders = true)
    }

    fun closeProviderCustomizer() {
        _uiState.value = _uiState.value.copy(isCustomizingProviders = false)
    }

    private suspend fun loadProviderCategoriesAndFeed(providerId: String, categoryId: String? = null) {
        _uiState.value = _uiState.value.copy(
            isLoadingCategories = true,
            isLoadingContent = true,
            errorMessage = null
        )

        val categoriesResult = catalogRepository.getCategories(providerId)
        val categories = categoriesResult.getOrDefault(emptyList())
        val activeCategory = categoryId
            ?: categories.firstOrNull()?.id
            ?: "all"

        _uiState.value = _uiState.value.copy(
            categories = categories,
            selectedCategoryId = activeCategory,
            isLoadingCategories = false
        )

        loadCategoryVideos(providerId, activeCategory)
    }

    private suspend fun loadCategoryVideos(providerId: String, categoryId: String) {
        _uiState.value = _uiState.value.copy(isLoadingContent = true, errorMessage = null)
        val videosResult = catalogRepository.getVideos(providerId, categoryId, 1)

        if (videosResult.isSuccess) {
            val items = videosResult.getOrDefault(emptyList())
            val spotlight = items.firstOrNull()
            val popular = if (items.isNotEmpty()) items.drop(1) else emptyList()

            val shortDramaProvider = _uiState.value.providers.firstOrNull { it.contentType == "short_drama" }
            val shortDramas = if (_uiState.value.selectedContentType == "short_drama") {
                items
            } else if (shortDramaProvider != null) {
                val shortCat = catalogRepository.getCategories(shortDramaProvider.id).getOrNull()?.firstOrNull()?.id ?: "all"
                catalogRepository.getVideos(shortDramaProvider.id, shortCat, 1).getOrDefault(emptyList())
            } else {
                emptyList()
            }

            _uiState.value = _uiState.value.copy(
                categoryVideos = items,
                spotlightItem = spotlight,
                popularVideos = popular,
                shortDramaVideos = shortDramas,
                isLoading = false,
                isLoadingContent = false,
                errorMessage = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoadingContent = false,
                errorMessage = videosResult.exceptionOrNull()?.localizedMessage ?: "Gagal memuat katalog video"
            )
        }
    }

    private fun buildContentTypes(providers: List<ProviderModel>): List<ContentTypeOption> {
        val preferredOrder = listOf("short_drama", "movie_tv", "long_drama", "live_tv")
        val grouped = providers.groupBy { it.contentType }
        val sortedTypes = grouped.keys.sortedBy { type ->
            val idx = preferredOrder.indexOf(type.lowercase())
            if (idx >= 0) idx else 99
        }
        return sortedTypes.map { type ->
            ContentTypeOption(
                id = type,
                label = getContentTypeLabel(type),
                count = grouped[type]?.size ?: 0
            )
        }
    }

    private fun getContentTypeLabel(type: String): String = when (type.lowercase()) {
        "short_drama" -> "Drama Pendek"
        "movie_tv" -> "Film & Serial"
        "live_tv" -> "Live TV"
        "long_drama" -> "Film & Serial"
        "anime" -> "Anime"
        else -> type.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
