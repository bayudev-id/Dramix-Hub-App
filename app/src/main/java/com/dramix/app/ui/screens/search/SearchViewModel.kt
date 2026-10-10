package com.dramix.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dramix.app.core.network.AppErrorSanitizer
import com.dramix.app.data.source.local.ProviderPreferences
import com.dramix.app.data.source.local.SearchPreferences
import com.dramix.app.domain.model.ProviderModel
import com.dramix.app.domain.model.VideoItem
import com.dramix.app.domain.repository.CatalogRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedContentType: String? = null,
    val selectedProviderId: String = "wetv",
    val availableProviders: List<ProviderModel> = emptyList(),
    val results: List<VideoItem> = emptyList(),
    val recentQueries: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val hasMoreResults: Boolean = false,
    val isLoadingMore: Boolean = false
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val catalogRepository: CatalogRepository,
    private val searchPreferences: SearchPreferences,
    private val providerPreferences: ProviderPreferences? = null
) : ViewModel() {

    companion object {
        val PREFERRED_PROVIDER_ORDER = listOf(
            "wetv",
            "moviebox",
            "viu",
            "kisskh",
            "iqiyi",
            "youku",
            "freereels"
        )
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryInputFlow = MutableStateFlow("")
    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        loadProviders()
        observeRecentQueries()
        setupDebouncedSearch()
    }

    private fun sortProviders(providers: List<ProviderModel>): List<ProviderModel> {
        return providers.sortedWith(
            compareBy<ProviderModel> { provider ->
                val idx = PREFERRED_PROVIDER_ORDER.indexOf(provider.id.lowercase())
                if (idx >= 0) idx else Int.MAX_VALUE
            }.thenBy { it.name.lowercase() }
        )
    }

    private fun loadProviders() {
        viewModelScope.launch {
            val providersResult = catalogRepository.getProviders()
            val rawProviders = (providersResult.getOrNull() ?: emptyList()).filter { it.isActive }
            val orderedProviders = sortProviders(rawProviders)
            val effectiveProviders = (providerPreferences?.applyToProviders(orderedProviders) ?: orderedProviders).filter { it.isActive }
            val defaultProvider = effectiveProviders.firstOrNull()?.id
                ?: "wetv"

            _uiState.value = _uiState.value.copy(
                availableProviders = effectiveProviders,
                selectedProviderId = defaultProvider
            )
        }
    }

    private fun observeRecentQueries() {
        viewModelScope.launch {
            searchPreferences.observeRecentQueries().collect { queries ->
                _uiState.value = _uiState.value.copy(recentQueries = queries)
            }
        }
    }

    private fun setupDebouncedSearch() {
        viewModelScope.launch {
            queryInputFlow
                .debounce(500L)
                .distinctUntilChanged()
                .collect { q ->
                    if (q.isNotBlank()) {
                        executeSearch(
                            query = q,
                            contentType = _uiState.value.selectedContentType,
                            providerId = _uiState.value.selectedProviderId,
                            saveToHistory = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            results = emptyList(),
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        queryInputFlow.value = newQuery
    }

    fun onContentTypeChange(contentType: String?) {
        if (_uiState.value.selectedContentType == contentType) return
        val currentQuery = _uiState.value.query.trim()
        val willSearch = currentQuery.isNotBlank()
        _uiState.value = _uiState.value.copy(
            selectedContentType = contentType,
            results = emptyList(),
            isLoading = willSearch,
            currentPage = 1,
            hasMoreResults = false,
            isLoadingMore = false,
            errorMessage = null
        )

        if (willSearch) {
            executeSearch(
                query = currentQuery,
                contentType = contentType,
                providerId = _uiState.value.selectedProviderId,
                saveToHistory = false,
                page = 1
            )
        }
    }

    fun onProviderChange(providerId: String) {
        if (_uiState.value.selectedProviderId == providerId) return
        val currentQuery = _uiState.value.query.trim()
        val willSearch = currentQuery.isNotBlank()
        _uiState.value = _uiState.value.copy(
            selectedProviderId = providerId,
            results = emptyList(),
            isLoading = willSearch,
            currentPage = 1,
            hasMoreResults = false,
            isLoadingMore = false,
            errorMessage = null
        )

        if (willSearch) {
            executeSearch(
                query = currentQuery,
                contentType = _uiState.value.selectedContentType,
                providerId = providerId,
                saveToHistory = false,
                page = 1
            )
        }
    }

    fun searchImmediately(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        _uiState.value = _uiState.value.copy(query = clean)
        queryInputFlow.value = ""
        executeSearch(
            query = clean,
            contentType = _uiState.value.selectedContentType,
            providerId = _uiState.value.selectedProviderId,
            saveToHistory = true
        )
    }

    fun selectRecentQuery(query: String) {
        searchImmediately(query)
    }

    fun deleteRecentQuery(query: String) {
        searchPreferences.removeQuery(query)
    }

    fun clearAllRecentQueries() {
        searchPreferences.clearAll()
    }

    private fun executeSearch(
        query: String,
        contentType: String?,
        providerId: String,
        saveToHistory: Boolean,
        page: Int = 1
    ) {
        val clean = query.trim()
        if (clean.isBlank()) return

        if (saveToHistory && page == 1) {
            searchPreferences.saveQuery(clean)
        }

        if (page == 1) {
            searchJob?.cancel()
        }

        val isInitialSearch = page == 1
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = isInitialSearch,
                isLoadingMore = !isInitialSearch,
                errorMessage = null
            )

            val result = catalogRepository.getSearchFeed(
                modelId = providerId,
                query = clean,
                page = page,
                contentType = contentType
            )

            if (result.isSuccess) {
                val searchFeed = result.getOrNull()
                val newItems = searchFeed?.items ?: emptyList()
                val hasMore = searchFeed?.hasMore ?: false
                val combinedResults = if (isInitialSearch) newItems else _uiState.value.results + newItems
                
                // Deduplicate by ID
                val seenIds = mutableSetOf<String>()
                val deduped = combinedResults.filter { item ->
                    if (seenIds.contains(item.id)) false else {
                        seenIds.add(item.id)
                        true
                    }
                }

                _uiState.value = _uiState.value.copy(
                    results = deduped,
                    isLoading = false,
                    isLoadingMore = false,
                    currentPage = page,
                    hasMoreResults = hasMore
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = AppErrorSanitizer.formatSearch(result.exceptionOrNull())
                )
            }
        }
    }

    fun loadMoreResults() {
        val query = _uiState.value.query
        val contentType = _uiState.value.selectedContentType
        val providerId = _uiState.value.selectedProviderId
        val nextPage = _uiState.value.currentPage + 1

        if (query.isBlank() || !_uiState.value.hasMoreResults || _uiState.value.isLoadingMore) return

        executeSearch(
            query = query,
            contentType = contentType,
            providerId = providerId,
            saveToHistory = false,
            page = nextPage
        )
    }
}
