package com.dramix.app.ui.screens.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dramix.app.core.database.dao.WatchHistoryDao
import com.dramix.app.core.database.entity.WatchHistoryEntity
import com.dramix.app.data.source.local.ProviderPreferences
import com.dramix.app.data.source.local.UserProviderConfig
import com.dramix.app.domain.model.Category
import com.dramix.app.domain.model.DramaDetail
import com.dramix.app.domain.model.PlaybackSource
import com.dramix.app.domain.model.ProviderModel
import com.dramix.app.domain.model.VideoFeedPage
import com.dramix.app.domain.model.VideoItem
import com.dramix.app.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var providerPreferences: ProviderPreferences

    private class FakeWatchHistoryDao : WatchHistoryDao {
        override suspend fun insertOrUpdateWatchHistory(history: WatchHistoryEntity): Long = 1L
        override suspend fun getEpisodeHistory(dramaId: String, providerId: String, episodeNumber: Int): WatchHistoryEntity? = null
        override suspend fun getLatestWatchedEpisode(dramaId: String, providerId: String): WatchHistoryEntity? = null
        override fun getDramaHistory(dramaId: String, providerId: String): Flow<List<WatchHistoryEntity>> = flowOf(emptyList())
        override fun getLatestWatchedDramas(limit: Int): Flow<List<WatchHistoryEntity>> = flowOf(
            listOf(
                WatchHistoryEntity(
                    id = 1,
                    dramaId = "drama-1",
                    providerId = "wetv",
                    dramaTitle = "Love In Contract",
                    episodeNumber = 2,
                    positionMs = 30000L,
                    durationMs = 60000L
                )
            )
        )
        override suspend fun deleteDramaHistory(dramaId: String, providerId: String): Int = 1
        override suspend fun clearAllHistory(): Int = 1
    }

    private class FakeCatalogRepository(
        var categoriesResult: Result<List<Category>> = Result.success(listOf(Category(id = "1001", name = "Untukmu"))),
        var lastRequestedCategoryId: String? = null
    ) : CatalogRepository {
        var getCategoriesCallCount = 0

        override suspend fun getProviders(): Result<List<ProviderModel>> = Result.success(
            listOf(
                ProviderModel(id = "wetv", name = "WeTV", contentType = "long_drama"),
                ProviderModel(id = "freereels", name = "FreeReels", contentType = "short_drama"),
                ProviderModel(id = "viu", name = "VIU", contentType = "long_drama")
            )
        )

        override suspend fun getCategories(modelId: String): Result<List<Category>> {
            getCategoriesCallCount++
            return categoriesResult
        }

        override suspend fun getVideos(modelId: String, categoryId: String, page: Int): Result<List<VideoItem>> {
            lastRequestedCategoryId = categoryId
            return Result.success(
                listOf(
                    VideoItem(id = "v-1", title = "Spotlight Drama", score = "9.5"),
                    VideoItem(id = "v-2", title = "Popular Drama 2", score = "8.8")
                )
            )
        }

        override suspend fun getVideoFeed(modelId: String, categoryId: String, page: Int): Result<VideoFeedPage> {
            lastRequestedCategoryId = categoryId
            return Result.success(
                VideoFeedPage(
                    items = listOf(
                        VideoItem(id = "v-1", title = "Spotlight Drama", score = "9.5"),
                        VideoItem(id = "v-2", title = "Popular Drama 2", score = "8.8")
                    ),
                    hasMore = false
                )
            )
        }

        override suspend fun getDramaDetail(modelId: String, id: String): Result<DramaDetail> =
            Result.failure(NotImplementedError())

        override suspend fun getPlaybackSource(modelId: String, episodeId: String, id: String?): Result<PlaybackSource> =
            Result.failure(NotImplementedError())

        override suspend fun search(modelId: String, query: String, page: Int, contentType: String?): Result<List<VideoItem>> =
            Result.success(emptyList())
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        providerPreferences = ProviderPreferences(context)
        providerPreferences.resetToDefault()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun homeViewModel_initializes_and_loads_default_feed() = runTest {
        val viewModel = HomeViewModel(
            catalogRepository = FakeCatalogRepository(),
            watchHistoryDao = FakeWatchHistoryDao(),
            providerPreferences = providerPreferences
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.providers.size)
        assertEquals("wetv", state.selectedProviderId)
        assertEquals(2, state.categoryVideos.size)
        assertFalse(state.hasMoreContent)
    }

    @Test
    fun homeViewModel_switches_provider_and_updates_state() = runTest {
        val viewModel = HomeViewModel(
            catalogRepository = FakeCatalogRepository(),
            watchHistoryDao = FakeWatchHistoryDao(),
            providerPreferences = providerPreferences
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectProvider("freereels")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("freereels", viewModel.uiState.value.selectedProviderId)
    }

    @Test
    fun homeViewModel_reorders_and_filters_providers_dynamically() = runTest {
        val viewModel = HomeViewModel(
            catalogRepository = FakeCatalogRepository(),
            watchHistoryDao = FakeWatchHistoryDao(),
            providerPreferences = providerPreferences
        )

        testDispatcher.scheduler.advanceUntilIdle()

        // User customizes: moves VIU first, FreeReels second, turns off WeTV
        val newConfigs = listOf(
            UserProviderConfig(id = "viu", isEnabled = true),
            UserProviderConfig(id = "freereels", isEnabled = true),
            UserProviderConfig(id = "wetv", isEnabled = false)
        )
        viewModel.updateProviderConfigs(newConfigs)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertEquals(2, updatedState.providers.size)
        assertEquals("viu", updatedState.providers[0].id)
        assertEquals("freereels", updatedState.providers[1].id)
        assertFalse(updatedState.providers.any { it.id == "wetv" })
        assertEquals("viu", updatedState.selectedProviderId)
    }

    @Test
    fun homeViewModel_hierarchical_content_types_and_categories_loaded() = runTest {
        val viewModel = HomeViewModel(
            catalogRepository = FakeCatalogRepository(),
            watchHistoryDao = FakeWatchHistoryDao(),
            providerPreferences = providerPreferences
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.contentTypes.size)
        assertTrue(state.contentTypes.any { it.id == "short_drama" && it.count == 1 })
        assertTrue(state.contentTypes.any { it.id == "long_drama" && it.count == 2 })
        assertEquals("long_drama", state.selectedContentType)
        assertEquals(2, state.filteredProviders.size)
        assertEquals("1001", state.selectedCategoryId)
        assertEquals(2, state.categoryVideos.size)
    }

    @Test
    fun homeViewModel_when_categories_fail_does_not_request_feed_with_all_and_shows_error() = runTest {
        val repo = FakeCatalogRepository(
            categoriesResult = Result.failure(IllegalStateException("Upstream timeout"))
        )
        val viewModel = HomeViewModel(
            catalogRepository = repo,
            watchHistoryDao = FakeWatchHistoryDao(),
            providerPreferences = providerPreferences
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingCategories)
        assertFalse(state.isLoadingContent)
        assertTrue(state.categories.isEmpty())
        org.junit.Assert.assertNull(state.selectedCategoryId)
        org.junit.Assert.assertNull("Harus tidak memanggil video feed dengan category 'all'", repo.lastRequestedCategoryId)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("ERR_CAT_001"))
    }

    @Test
    fun homeViewModel_when_categories_empty_does_not_request_feed_and_shows_empty_error() = runTest {
        val repo = FakeCatalogRepository(
            categoriesResult = Result.success(emptyList())
        )
        val viewModel = HomeViewModel(
            catalogRepository = repo,
            watchHistoryDao = FakeWatchHistoryDao(),
            providerPreferences = providerPreferences
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingCategories)
        assertFalse(state.isLoadingContent)
        assertTrue(state.categories.isEmpty())
        org.junit.Assert.assertNull(state.selectedCategoryId)
        org.junit.Assert.assertNull("Harus tidak memanggil video feed dengan category 'all'", repo.lastRequestedCategoryId)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun homeViewModel_refresh_retries_categories_when_categories_were_empty_or_failed() = runTest {
        val repo = FakeCatalogRepository(
            categoriesResult = Result.failure(IllegalStateException("First try failed"))
        )
        val viewModel = HomeViewModel(
            catalogRepository = repo,
            watchHistoryDao = FakeWatchHistoryDao(),
            providerPreferences = providerPreferences
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repo.getCategoriesCallCount)
        assertNotNull(viewModel.uiState.value.errorMessage)

        // Now categories become available, user taps retry / refresh
        repo.categoriesResult = Result.success(listOf(Category(id = "1001", name = "Untukmu")))
        viewModel.refreshCurrentCategory()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, repo.getCategoriesCallCount)
        assertEquals("1001", repo.lastRequestedCategoryId)
        assertEquals("1001", viewModel.uiState.value.selectedCategoryId)
        assertEquals(2, viewModel.uiState.value.categoryVideos.size)
        org.junit.Assert.assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun homeViewModel_select_content_type_switches_providers_and_feed() = runTest {
        val viewModel = HomeViewModel(
            catalogRepository = FakeCatalogRepository(),
            watchHistoryDao = FakeWatchHistoryDao(),
            providerPreferences = providerPreferences
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectContentType("short_drama")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("short_drama", state.selectedContentType)
        assertEquals("freereels", state.selectedProviderId)
        assertEquals(1, state.filteredProviders.size)
        assertEquals("freereels", state.filteredProviders[0].id)
    }

    @Test
    fun homeViewModel_resets_provider_preferences_to_default() = runTest {
        val viewModel = HomeViewModel(
            catalogRepository = FakeCatalogRepository(),
            watchHistoryDao = FakeWatchHistoryDao(),
            providerPreferences = providerPreferences
        )

        testDispatcher.scheduler.advanceUntilIdle()

        // Customize then reset
        viewModel.updateProviderConfigs(listOf(UserProviderConfig("viu", true)))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetProviderConfigs()
        testDispatcher.scheduler.advanceUntilIdle()

        val resetState = viewModel.uiState.value
        assertEquals(3, resetState.providers.size)
        assertEquals("wetv", resetState.providers[0].id)
    }
}
