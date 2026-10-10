package com.dramix.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dramix.app.core.database.dao.WatchHistoryDao
import com.dramix.app.core.database.entity.WatchHistoryEntity
import com.dramix.app.data.source.local.ProviderConfigItem
import com.dramix.app.core.network.AppErrorSanitizer
import com.dramix.app.data.source.local.ProviderPreferences
import com.dramix.app.data.source.local.UserProviderConfig
import com.dramix.app.domain.model.Category
import com.dramix.app.domain.model.ProviderModel
import com.dramix.app.domain.model.VideoFeedPage
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
    val isLoadingMore: Boolean = false,
    val hasMoreContent: Boolean = false,
    val currentPage: Int = 1,
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
                    errorMessage = AppErrorSanitizer.formatCatalog(providersResult.exceptionOrNull(), defaultCode = "ERR_CAT_001")
                )
                return@launch
            }

            rawProviders = providersResult.getOrDefault(emptyList())
            val effectiveProviders = providerPreferences.applyToProviders(rawProviders)
            val contentTypes = buildContentTypes(effectiveProviders)

            // Restore last selection from preferences
            val lastContentType = providerPreferences.getLastContentType()
            val lastProviderId = providerPreferences.getLastProviderId()
            val lastCategoryId = providerPreferences.getLastCategoryId()

            val selectedContentType = if (lastContentType != null && contentTypes.any { it.id == lastContentType }) {
                lastContentType
            } else {
                effectiveProviders.firstOrNull()?.contentType ?: contentTypes.firstOrNull()?.id
            }

            val filteredProviders = if (selectedContentType != null) {
                effectiveProviders.filter { it.contentType == selectedContentType }
            } else {
                effectiveProviders
            }

            val defaultProvider = if (lastProviderId != null && filteredProviders.any { it.id == lastProviderId }) {
                filteredProviders.first { it.id == lastProviderId }
            } else {
                filteredProviders.firstOrNull()
            }

            _uiState.value = _uiState.value.copy(
                contentTypes = contentTypes,
                selectedContentType = selectedContentType,
                providers = effectiveProviders,
                filteredProviders = filteredProviders,
                selectedProviderId = defaultProvider?.id
            )

            if (defaultProvider != null) {
                loadProviderCategoriesAndFeed(defaultProvider.id, categoryId = lastCategoryId)
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

        providerPreferences.saveLastSelection(
            contentType = contentType,
            providerId = newSelectedProvider,
            categoryId = null
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
            currentPage = 1,
            hasMoreContent = false,
            isLoadingMore = false,
            isLoadingContent = true
        )

        providerPreferences.saveLastSelection(
            contentType = contentType,
            providerId = providerId,
            categoryId = null
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
            categoryVideos = emptyList(),
            currentPage = 1,
            hasMoreContent = false,
            isLoadingMore = false,
            isLoadingContent = true
        )

        providerPreferences.saveLastSelection(
            contentType = _uiState.value.selectedContentType,
            providerId = providerId,
            categoryId = categoryId
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
        
        // Validasi categoryId: jika disediakan tapi tidak ada di list, gunakan kategori pertama
        val validCategoryId = if (categoryId != null && categories.any { it.id == categoryId }) {
            categoryId
        } else {
            categories.firstOrNull()?.id ?: "all"
        }

        _uiState.value = _uiState.value.copy(
            categories = categories,
            selectedCategoryId = validCategoryId,
            isLoadingCategories = false
        )
        
        // Sync preferences dengan kategori yang valid
        providerPreferences.saveLastSelection(
            contentType = _uiState.value.selectedContentType,
            providerId = providerId,
            categoryId = validCategoryId
        )

        loadCategoryVideos(providerId, validCategoryId)
    }

    fun refreshCurrentCategory() {
        val providerId = _uiState.value.selectedProviderId ?: return
        val categoryId = _uiState.value.selectedCategoryId ?: return

        _uiState.value = _uiState.value.copy(
            categoryVideos = emptyList(),
            currentPage = 1,
            hasMoreContent = false,
            isLoadingMore = false,
            isLoadingContent = true
        )

        viewModelScope.launch {
            loadCategoryVideos(providerId, categoryId)
        }
    }

    private suspend fun loadCategoryVideos(providerId: String, categoryId: String) {
        _uiState.value = _uiState.value.copy(
            isLoadingContent = true,
            errorMessage = null,
            currentPage = 1,
            hasMoreContent = false,
            isLoadingMore = false
        )
        val feedResult = catalogRepository.getVideoFeed(providerId, categoryId, 1)

        if (feedResult.isSuccess) {
            val feed = feedResult.getOrDefault(VideoFeedPage())
            val items = feed.items
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
                currentPage = 1,
                hasMoreContent = feed.hasMore,
                isLoading = false,
                isLoadingContent = false,
                isLoadingMore = false,
                errorMessage = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoadingContent = false,
                isLoadingMore = false,
                errorMessage = AppErrorSanitizer.formatCatalog(feedResult.exceptionOrNull(), defaultCode = "ERR_CAT_002")
            )
        }
    }

    fun loadMoreVideos() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMoreContent || currentState.isLoadingContent) return

        val providerId = currentState.selectedProviderId ?: return
        val categoryId = currentState.selectedCategoryId ?: return
        val nextPage = currentState.currentPage + 1

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            val result = catalogRepository.getVideoFeed(providerId, categoryId, nextPage)
            if (result.isSuccess) {
                val latest = _uiState.value
                if (latest.selectedProviderId != providerId ||
                    latest.selectedCategoryId != categoryId
                ) {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                    return@launch
                }

                val feed = result.getOrDefault(VideoFeedPage())
                val existingIds = currentState.categoryVideos.map { it.id }.toSet()
                val newUniqueItems = feed.items.filter { it.id !in existingIds }
                val actuallyHasMore = feed.hasMore && newUniqueItems.isNotEmpty()

                _uiState.value = _uiState.value.copy(
                    categoryVideos = currentState.categoryVideos + newUniqueItems,
                    currentPage = nextPage,
                    hasMoreContent = actuallyHasMore,
                    isLoadingMore = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
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
