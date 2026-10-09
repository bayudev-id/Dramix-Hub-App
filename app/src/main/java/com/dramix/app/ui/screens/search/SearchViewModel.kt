package com.dramix.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val errorMessage: String? = null
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
            val rawProviders = providersResult.getOrNull() ?: emptyList()
            val orderedProviders = sortProviders(rawProviders)
            val effectiveProviders = providerPreferences?.applyToProviders(orderedProviders) ?: orderedProviders
            val defaultProvider = effectiveProviders.firstOrNull { it.status == "active" }?.id
                ?: effectiveProviders.firstOrNull()?.id
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
        _uiState.value = _uiState.value.copy(selectedContentType = contentType)

        val currentQuery = _uiState.value.query.trim()
        if (currentQuery.isNotBlank()) {
            executeSearch(
                query = currentQuery,
                contentType = contentType,
                providerId = _uiState.value.selectedProviderId,
                saveToHistory = false
            )
        }
    }

    fun onProviderChange(providerId: String) {
        if (_uiState.value.selectedProviderId == providerId) return
        _uiState.value = _uiState.value.copy(selectedProviderId = providerId)

        val currentQuery = _uiState.value.query.trim()
        if (currentQuery.isNotBlank()) {
            executeSearch(
                query = currentQuery,
                contentType = _uiState.value.selectedContentType,
                providerId = providerId,
                saveToHistory = false
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
        saveToHistory: Boolean
    ) {
        val clean = query.trim()
        if (clean.isBlank()) return

        if (saveToHistory) {
            searchPreferences.saveQuery(clean)
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = catalogRepository.search(
                modelId = providerId,
                query = clean,
                page = 1,
                contentType = contentType
            )

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    results = result.getOrNull() ?: emptyList(),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    results = emptyList(),
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Pencarian gagal"
                )
            }
        }
    }
}
