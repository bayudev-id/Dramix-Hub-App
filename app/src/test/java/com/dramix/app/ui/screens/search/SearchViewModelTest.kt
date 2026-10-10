package com.dramix.app.ui.screens.search

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dramix.app.data.source.local.SearchPreferences
import com.dramix.app.domain.model.Category
import com.dramix.app.domain.model.DramaDetail
import com.dramix.app.domain.model.PlaybackSource
import com.dramix.app.domain.model.ProviderModel
import com.dramix.app.domain.model.VideoItem
import com.dramix.app.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var searchPreferences: SearchPreferences

    private val mockProviders = listOf(
        ProviderModel(id = "wetv", name = "WeTV", contentType = "long_drama"),
        ProviderModel(id = "dramabox", name = "DramaBox", contentType = "short_drama")
    )

    private val mockSearchResults = listOf(
        VideoItem(id = "v-1", title = "Love Like the Galaxy", cover = "https://example.com/cover1.jpg", type = "long_drama"),
        VideoItem(id = "v-2", title = "First Love Again", cover = "https://example.com/cover2.jpg", type = "long_drama")
    )

    private class FakeSearchCatalogRepository(
        private val providers: List<ProviderModel>,
        private val searchResults: List<VideoItem>
    ) : CatalogRepository {

        var lastSearchModelId: String? = null
        var lastSearchQuery: String? = null
        var lastSearchContentType: String? = null

        override suspend fun getProviders(): Result<List<ProviderModel>> = Result.success(providers)

        override suspend fun getCategories(modelId: String): Result<List<Category>> = Result.success(emptyList())

        override suspend fun getVideos(modelId: String, categoryId: String, page: Int): Result<List<VideoItem>> = Result.success(emptyList())

        override suspend fun getDramaDetail(modelId: String, id: String): Result<DramaDetail> = Result.failure(UnsupportedOperationException())

        override suspend fun getPlaybackSource(modelId: String, episodeId: String, id: String?): Result<PlaybackSource> = Result.failure(UnsupportedOperationException())

        override suspend fun search(
            modelId: String,
            query: String,
            page: Int,
            contentType: String?
        ): Result<List<VideoItem>> {
            lastSearchModelId = modelId
            lastSearchQuery = query
            lastSearchContentType = contentType

            val filtered = searchResults.filter { it.title.contains(query, ignoreCase = true) }
            return Result.success(filtered)
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        searchPreferences = SearchPreferences(context)
        searchPreferences.clearAll()
    }

    @After
    fun tearDown() {
        searchPreferences.clearAll()
        Dispatchers.resetMain()
    }

    @Test
    fun search_initializes_with_providers_and_default_state() = runTest {
        val repo = FakeSearchCatalogRepository(mockProviders, mockSearchResults)
        val viewModel = SearchViewModel(repo, searchPreferences)

        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals("", state.query)
        assertEquals("wetv", state.selectedProviderId)
        assertEquals(2, state.availableProviders.size)
        assertTrue(state.results.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun search_debounces_query_and_updates_results_and_history() = runTest {
        val repo = FakeSearchCatalogRepository(mockProviders, mockSearchResults)
        val viewModel = SearchViewModel(repo, searchPreferences)

        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        // Type query "Love"
        viewModel.onQueryChange("Love")
        assertEquals("Love", viewModel.uiState.value.query)

        // Before 500ms debounce expires: search has not fired yet
        testDispatcher.scheduler.advanceTimeBy(200)
        testDispatcher.scheduler.runCurrent()
        assertEquals(null, repo.lastSearchQuery)

        // Advance past 500ms debounce
        testDispatcher.scheduler.advanceTimeBy(400)
        testDispatcher.scheduler.runCurrent()

        assertEquals("Love", repo.lastSearchQuery)
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.results.size)
        assertEquals("Love Like the Galaxy", state.results[0].title)

        // Verify query was recorded in recent queries
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.recentQueries.contains("Love"))
    }

    @Test
    fun search_filters_by_content_type_and_provider() = runTest {
        val repo = FakeSearchCatalogRepository(mockProviders, mockSearchResults)
        val viewModel = SearchViewModel(repo, searchPreferences)

        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        // Immediate search
        viewModel.searchImmediately("Love")
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        assertEquals("wetv", repo.lastSearchModelId)
        assertEquals(null, repo.lastSearchContentType)

        // Change content type to short_drama
        viewModel.onContentTypeChange("short_drama")
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        assertEquals("short_drama", repo.lastSearchContentType)
        assertEquals("short_drama", viewModel.uiState.value.selectedContentType)

        // Change provider to dramabox
        viewModel.onProviderChange("dramabox")
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        assertEquals("dramabox", repo.lastSearchModelId)
        assertEquals("dramabox", viewModel.uiState.value.selectedProviderId)
        assertEquals(1, viewModel.uiState.value.currentPage)
    }

    @Test
    fun switching_provider_resets_page_and_clears_old_results() = runTest {
        val repo = FakeSearchCatalogRepository(mockProviders, mockSearchResults)
        val viewModel = SearchViewModel(repo, searchPreferences)

        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        // Search on default provider (wetv)
        viewModel.searchImmediately("Love")
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        assertEquals("wetv", repo.lastSearchModelId)
        assertEquals(2, viewModel.uiState.value.results.size)

        // Switch to dramabox while scrolled / with active results
        viewModel.onProviderChange("dramabox")

        // Immediately after change: state is reset to page 1
        assertEquals("dramabox", viewModel.uiState.value.selectedProviderId)
        assertEquals(1, viewModel.uiState.value.currentPage)

        // After fetch completes
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        assertEquals("dramabox", repo.lastSearchModelId)
        assertEquals(1, viewModel.uiState.value.currentPage)
    }

    @Test
    fun search_manages_recent_history_chips() = runTest {
        val repo = FakeSearchCatalogRepository(mockProviders, mockSearchResults)
        val viewModel = SearchViewModel(repo, searchPreferences)

        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        searchPreferences.saveQuery("Historical Drama")
        searchPreferences.saveQuery("Romance 2024")

        testDispatcher.scheduler.advanceTimeBy(200)
        testDispatcher.scheduler.runCurrent()

        assertEquals(2, viewModel.uiState.value.recentQueries.size)

        // Select a recent query chip
        viewModel.selectRecentQuery("Historical Drama")
        testDispatcher.scheduler.advanceTimeBy(200)
        testDispatcher.scheduler.runCurrent()

        assertEquals("Historical Drama", viewModel.uiState.value.query)
        assertEquals("Historical Drama", repo.lastSearchQuery)

        // Delete a query
        viewModel.deleteRecentQuery("Romance 2024")
        testDispatcher.scheduler.advanceTimeBy(200)
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, viewModel.uiState.value.recentQueries.size)
        assertFalse(viewModel.uiState.value.recentQueries.contains("Romance 2024"))

        // Clear all queries
        viewModel.clearAllRecentQueries()
        testDispatcher.scheduler.advanceTimeBy(200)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.recentQueries.isEmpty())
    }

    @Test
    fun search_handles_special_characters_and_clearing() = runTest {
        val repo = FakeSearchCatalogRepository(mockProviders, mockSearchResults)
        val viewModel = SearchViewModel(repo, searchPreferences)

        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        // Special characters and emojis
        viewModel.onQueryChange("@#$%^&*()_+ 😊🚀")
        testDispatcher.scheduler.advanceTimeBy(600)
        testDispatcher.scheduler.runCurrent()

        assertEquals("@#$%^&*()_+ 😊🚀", repo.lastSearchQuery)
        assertTrue(viewModel.uiState.value.results.isEmpty())

        // Clear query
        viewModel.onQueryChange("")
        testDispatcher.scheduler.advanceTimeBy(600)
        testDispatcher.scheduler.runCurrent()

        assertEquals("", viewModel.uiState.value.query)
        assertTrue(viewModel.uiState.value.results.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun search_orders_providers_according_to_preferred_order() = runTest {
        val unorderedProviders = listOf(
            ProviderModel(id = "netshort", name = "NetShort"),
            ProviderModel(id = "freereels", name = "FreeReels"),
            ProviderModel(id = "kisskh", name = "KissKH"),
            ProviderModel(id = "wetv", name = "WeTV"),
            ProviderModel(id = "moviebox", name = "MovieBox"),
            ProviderModel(id = "iqiyi", name = "iQIYI"),
            ProviderModel(id = "viu", name = "VIU")
        )
        val repo = FakeSearchCatalogRepository(unorderedProviders, emptyList())
        val viewModel = SearchViewModel(repo, searchPreferences)

        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        val orderedIds = viewModel.uiState.value.availableProviders.map { it.id }
        assertEquals(
            listOf("wetv", "moviebox", "viu", "kisskh", "iqiyi", "freereels", "netshort"),
            orderedIds
        )
        assertEquals("wetv", viewModel.uiState.value.selectedProviderId)
    }
}
