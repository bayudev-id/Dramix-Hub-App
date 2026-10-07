package com.dramix.app.ui.screens.player_shorts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dramix.app.core.database.dao.BookmarkDao
import com.dramix.app.core.database.dao.WatchHistoryDao
import com.dramix.app.core.database.entity.BookmarkEntity
import com.dramix.app.core.database.entity.WatchHistoryEntity
import com.dramix.app.domain.manager.EntitlementManager
import com.dramix.app.domain.model.Category
import com.dramix.app.domain.model.DramaDetail
import com.dramix.app.domain.model.Episode
import com.dramix.app.domain.model.LicenseStatus
import com.dramix.app.domain.model.PlaybackSource
import com.dramix.app.domain.model.ProviderModel
import com.dramix.app.domain.model.Season
import com.dramix.app.domain.model.StreamSource
import com.dramix.app.domain.model.VideoItem
import com.dramix.app.domain.repository.CatalogRepository
import com.dramix.app.domain.repository.LicenseRepository
import com.dramix.app.player.engine.PlayerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
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
class ShortsPlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var playerFactory: PlayerFactory

    private val mockShortDramaDetail = DramaDetail(
        id = "short-101",
        title = "CEO Secret Bride",
        cover = "https://example.com/cover.jpg",
        description = "Dramatic romantic short drama in 9:16 format",
        totalEpisodes = 60,
        score = "9.1",
        seasons = listOf(
            Season(
                name = "Season 1",
                index = 1,
                totalEpisodes = 60,
                episodes = (1..60).map { epNum ->
                    Episode(
                        id = "ep-$epNum",
                        title = "Ep. $epNum",
                        number = epNum,
                        isVip = epNum >= 4
                    )
                }
            )
        )
    )

    private class FakeCatalogRepository(private val detail: DramaDetail) : CatalogRepository {
        override suspend fun getProviders(): Result<List<ProviderModel>> = Result.success(
            listOf(
                ProviderModel(id = "freereels", name = "FreeReels", contentType = "short_drama")
            )
        )
        override suspend fun getCategories(modelId: String): Result<List<Category>> = Result.success(emptyList())
        override suspend fun getVideos(modelId: String, categoryId: String, page: Int): Result<List<VideoItem>> = Result.success(
            listOf(
                VideoItem(id = "short-101", title = "CEO Secret Bride")
            )
        )
        override suspend fun getDramaDetail(modelId: String, id: String): Result<DramaDetail> = Result.success(detail)
        override suspend fun getPlaybackSource(modelId: String, episodeId: String, id: String?): Result<PlaybackSource> = Result.success(
            PlaybackSource(
                id = id ?: episodeId,
                episodeId = episodeId,
                durationSeconds = 120,
                streams = listOf(
                    StreamSource(
                        quality = "720p",
                        format = "mp4",
                        url = "https://cdn.example.com/short_$episodeId.mp4",
                        headers = mapOf("Referer" to "https://freereels.cc/")
                    )
                )
            )
        )
        override suspend fun search(modelId: String, query: String, page: Int, contentType: String?): Result<List<VideoItem>> = Result.success(emptyList())
    }

    private class FakeWatchHistoryDao : WatchHistoryDao {
        val records = mutableMapOf<String, WatchHistoryEntity>()

        override suspend fun insertOrUpdateWatchHistory(history: WatchHistoryEntity): Long {
            val key = "${history.dramaId}_${history.providerId}_${history.episodeNumber}"
            records[key] = history
            return 1L
        }

        override suspend fun getEpisodeHistory(dramaId: String, providerId: String, episodeNumber: Int): WatchHistoryEntity? {
            return records["${dramaId}_${providerId}_$episodeNumber"]
        }

        override fun getDramaHistory(dramaId: String, providerId: String): Flow<List<WatchHistoryEntity>> = flowOf(emptyList())
        override fun getLatestWatchedDramas(limit: Int): Flow<List<WatchHistoryEntity>> = flowOf(emptyList())
        override suspend fun deleteDramaHistory(dramaId: String, providerId: String): Int = 1
        override suspend fun clearAllHistory(): Int = 1
    }

    private class FakeBookmarkDao : BookmarkDao {
        val isBookmarkedState = MutableStateFlow(false)

        override suspend fun insertBookmark(bookmark: BookmarkEntity): Long {
            isBookmarkedState.value = true
            return 1L
        }

        override suspend fun deleteBookmark(dramaId: String, providerId: String): Int {
            isBookmarkedState.value = false
            return 1
        }

        override fun getAllBookmarks(): Flow<List<BookmarkEntity>> = flowOf(emptyList())
        override fun isBookmarked(dramaId: String, providerId: String): Flow<Boolean> = isBookmarkedState
        override suspend fun getBookmark(dramaId: String, providerId: String): BookmarkEntity? = null
    }

    private class FakeLicenseRepository(var vipActive: Boolean = false) : LicenseRepository {
        override suspend fun activateLicense(licenseKey: String): Result<LicenseStatus> = Result.success(LicenseStatus(isVip = true))
        override suspend fun refreshLicenseStatus(): Result<LicenseStatus> = Result.success(LicenseStatus(isVip = vipActive))
        override fun isVipActive(): Boolean = vipActive
        override fun getSessionToken(): String? = if (vipActive) "token" else null
        override fun observeVipStatus(): Flow<Boolean> = flowOf(vipActive)
        override fun clearLicense() { vipActive = false }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        playerFactory = PlayerFactory(context, OkHttpClient())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun shortsPlayer_loads_feed_and_plays_first_episode() = runTest {
        val watchHistoryDao = FakeWatchHistoryDao()
        val bookmarkDao = FakeBookmarkDao()
        val licenseRepo = FakeLicenseRepository(vipActive = false)
        val entitlementManager = EntitlementManager(licenseRepo)

        val viewModel = ShortsPlayerViewModel(
            initialProviderId = "freereels",
            initialDramaId = "short-101",
            catalogRepository = FakeCatalogRepository(mockShortDramaDetail),
            watchHistoryDao = watchHistoryDao,
            bookmarkDao = bookmarkDao,
            entitlementManager = entitlementManager,
            playerFactory = playerFactory,
            enablePlayerCache = false
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("CEO Secret Bride", state.detail?.title)
        assertEquals(60, state.episodes.size)
        assertEquals(0, state.currentEpisodeIndex)
        assertNotNull(state.currentPlaybackSource)
        assertEquals("https://cdn.example.com/short_ep-1.mp4", state.currentPlaybackSource?.streams?.firstOrNull()?.url)

        viewModel.release()
    }

    @Test
    fun shortsPlayer_shows_license_gate_for_episode_4_on_non_vip() = runTest {
        val watchHistoryDao = FakeWatchHistoryDao()
        val bookmarkDao = FakeBookmarkDao()
        val licenseRepo = FakeLicenseRepository(vipActive = false)
        val entitlementManager = EntitlementManager(licenseRepo)

        val viewModel = ShortsPlayerViewModel(
            initialProviderId = "freereels",
            initialDramaId = "short-101",
            catalogRepository = FakeCatalogRepository(mockShortDramaDetail),
            watchHistoryDao = watchHistoryDao,
            bookmarkDao = bookmarkDao,
            entitlementManager = entitlementManager,
            playerFactory = playerFactory,
            enablePlayerCache = false
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        // Jump to index 3 (which is Episode 4)
        viewModel.onEpisodeSelected(3)
        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.showLicenseGate)
        assertEquals(4, state.lockedEpisodeNumber)

        viewModel.release()
    }

    @Test
    fun shortsPlayer_toggles_bookmark() = runTest {
        val watchHistoryDao = FakeWatchHistoryDao()
        val bookmarkDao = FakeBookmarkDao()
        val licenseRepo = FakeLicenseRepository(vipActive = true)
        val entitlementManager = EntitlementManager(licenseRepo)

        val viewModel = ShortsPlayerViewModel(
            initialProviderId = "freereels",
            initialDramaId = "short-101",
            catalogRepository = FakeCatalogRepository(mockShortDramaDetail),
            watchHistoryDao = watchHistoryDao,
            bookmarkDao = bookmarkDao,
            entitlementManager = entitlementManager,
            playerFactory = playerFactory,
            enablePlayerCache = false
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isBookmarked)

        viewModel.toggleBookmark()
        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isBookmarked)

        viewModel.toggleBookmark()
        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.uiState.value.isBookmarked)

        viewModel.release()
    }
}
