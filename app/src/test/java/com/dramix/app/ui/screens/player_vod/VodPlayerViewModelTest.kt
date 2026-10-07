package com.dramix.app.ui.screens.player_vod

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dramix.app.core.database.dao.BookmarkDao
import com.dramix.app.core.database.dao.DownloadRecordDao
import com.dramix.app.core.database.dao.WatchHistoryDao
import com.dramix.app.core.database.entity.BookmarkEntity
import com.dramix.app.core.database.entity.DownloadRecordEntity
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
class VodPlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var playerFactory: PlayerFactory

    private val mockDetail = DramaDetail(
        id = "drama-101",
        title = "Love Between Fairy and Devil",
        cover = "https://example.com/cover.jpg",
        description = "Fantasy romance drama",
        totalEpisodes = 24,
        score = "9.5",
        seasons = listOf(
            Season(
                name = "Season 1",
                index = 1,
                totalEpisodes = 24,
                episodes = listOf(
                    Episode(id = "ep-1", title = "Ep 1", number = 1, isVip = false),
                    Episode(id = "ep-2", title = "Ep 2", number = 2, isVip = false),
                    Episode(id = "ep-4", title = "Ep 4", number = 4, isVip = true)
                )
            )
        )
    )

    private class FakeCatalogRepository(private val detail: DramaDetail) : CatalogRepository {
        override suspend fun getProviders(): Result<List<ProviderModel>> = Result.success(emptyList())
        override suspend fun getCategories(modelId: String): Result<List<Category>> = Result.success(emptyList())
        override suspend fun getVideos(modelId: String, categoryId: String, page: Int): Result<List<VideoItem>> = Result.success(emptyList())
        override suspend fun getDramaDetail(modelId: String, id: String): Result<DramaDetail> = Result.success(detail)
        override suspend fun getPlaybackSource(modelId: String, episodeId: String, id: String?): Result<PlaybackSource> = Result.success(
            PlaybackSource(
                id = id ?: episodeId,
                episodeId = episodeId,
                durationSeconds = 2700,
                streams = listOf(
                    StreamSource(
                        quality = "1080p",
                        format = "m3u8",
                        url = "https://cdn.example.com/stream.m3u8",
                        headers = mapOf("Referer" to "https://wetv.vip/")
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

        override fun getDramaHistory(dramaId: String, providerId: String): Flow<List<WatchHistoryEntity>> {
            return flowOf(records.values.toList())
        }

        override fun getLatestWatchedDramas(limit: Int): Flow<List<WatchHistoryEntity>> {
            return flowOf(records.values.toList())
        }

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

    private class FakeDownloadRecordDao : DownloadRecordDao {
        val records = mutableMapOf<String, DownloadRecordEntity>()

        override suspend fun insertOrUpdateDownload(record: DownloadRecordEntity): Long {
            records[record.mediaId] = record
            return 1L
        }

        override suspend fun getDownloadByMediaIdSync(mediaId: String): DownloadRecordEntity? {
            return records[mediaId]
        }

        override fun getDownloadByMediaId(mediaId: String): Flow<DownloadRecordEntity?> =
            flowOf(records[mediaId])

        override fun getAllDownloads(): Flow<List<DownloadRecordEntity>> =
            flowOf(records.values.toList())

        override fun getDownloadsByStatus(status: String): Flow<List<DownloadRecordEntity>> =
            flowOf(records.values.filter { it.status.equals(status, ignoreCase = true) })

        override suspend fun updateDownloadProgress(
            mediaId: String,
            bytesDownloaded: Long,
            totalBytes: Long,
            progressPercentage: Int,
            status: String
        ): Int = 1

        override suspend fun updateDownloadStatus(
            mediaId: String,
            status: String,
            completedAt: Long?,
            errorMessage: String?
        ): Int = 1

        override suspend fun updateDownloadCompleted(
            mediaId: String,
            localUri: String,
            status: String,
            completedAt: Long
        ): Int = 1

        override suspend fun deleteDownload(mediaId: String): Int {
            return if (records.remove(mediaId) != null) 1 else 0
        }
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
    fun vodPlayer_initializes_and_plays_first_free_episode() = runTest {
        val watchHistoryDao = FakeWatchHistoryDao()
        val bookmarkDao = FakeBookmarkDao()
        val licenseRepository = FakeLicenseRepository(vipActive = false)
        val entitlementManager = EntitlementManager(licenseRepository)

        val viewModel = VodPlayerViewModel(
            providerId = "wetv",
            dramaId = "drama-101",
            catalogRepository = FakeCatalogRepository(mockDetail),
            watchHistoryDao = watchHistoryDao,
            bookmarkDao = bookmarkDao,
            entitlementManager = entitlementManager,
            licenseRepository = licenseRepository,
            playerFactory = playerFactory,
            enablePlayerCache = false
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingDetail)
        assertEquals("Love Between Fairy and Devil", state.detail?.title)
        assertEquals(1, state.currentEpisode?.number)
        assertFalse(state.showLicenseGate)
        assertNotNull(state.playbackSource)
        assertEquals("https://cdn.example.com/stream.m3u8", state.playbackSource?.streams?.firstOrNull()?.url)

        viewModel.release()
    }

    @Test
    fun vodPlayer_blocks_episode_4_for_non_vip_and_shows_license_dialog() = runTest {
        val watchHistoryDao = FakeWatchHistoryDao()
        val bookmarkDao = FakeBookmarkDao()
        val licenseRepository = FakeLicenseRepository(vipActive = false)
        val entitlementManager = EntitlementManager(licenseRepository)

        val viewModel = VodPlayerViewModel(
            providerId = "wetv",
            dramaId = "drama-101",
            catalogRepository = FakeCatalogRepository(mockDetail),
            watchHistoryDao = watchHistoryDao,
            bookmarkDao = bookmarkDao,
            entitlementManager = entitlementManager,
            licenseRepository = licenseRepository,
            playerFactory = playerFactory,
            enablePlayerCache = false
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        // Select Episode 4
        val ep4 = mockDetail.seasons.first().episodes.find { it.number == 4 }!!
        viewModel.playEpisode(ep4)

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.showLicenseGate)
        assertEquals(4, state.lockedEpisodeNumber)

        viewModel.release()
    }

    @Test
    fun vodPlayer_toggles_bookmark_reactively() = runTest {
        val watchHistoryDao = FakeWatchHistoryDao()
        val bookmarkDao = FakeBookmarkDao()
        val licenseRepository = FakeLicenseRepository(vipActive = true)
        val entitlementManager = EntitlementManager(licenseRepository)

        val viewModel = VodPlayerViewModel(
            providerId = "wetv",
            dramaId = "drama-101",
            catalogRepository = FakeCatalogRepository(mockDetail),
            watchHistoryDao = watchHistoryDao,
            bookmarkDao = bookmarkDao,
            entitlementManager = entitlementManager,
            licenseRepository = licenseRepository,
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

    @Test
    fun vodPlayer_plays_offline_downloaded_episode_without_network_request() = runTest {
        val watchHistoryDao = FakeWatchHistoryDao()
        val bookmarkDao = FakeBookmarkDao()
        val downloadRecordDao = FakeDownloadRecordDao()
        val licenseRepository = FakeLicenseRepository(vipActive = true)
        val entitlementManager = EntitlementManager(licenseRepository)

        // Seed a completed offline download for Episode 1
        val offlineUri = "file:///data/user/0/com.dramix.app/files/dramix_downloads/ep-1.mp4"
        downloadRecordDao.insertOrUpdateDownload(
            DownloadRecordEntity(
                mediaId = "ep-1",
                dramaId = "drama-101",
                providerId = "wetv",
                dramaTitle = "Love Between Fairy and Devil",
                episodeNumber = 1,
                streamUrl = "https://cdn.example.com/stream.m3u8",
                localUri = offlineUri,
                status = "COMPLETED"
            )
        )

        val viewModel = VodPlayerViewModel(
            providerId = "wetv",
            dramaId = "drama-101",
            catalogRepository = FakeCatalogRepository(mockDetail),
            watchHistoryDao = watchHistoryDao,
            bookmarkDao = bookmarkDao,
            entitlementManager = entitlementManager,
            licenseRepository = licenseRepository,
            playerFactory = playerFactory,
            enablePlayerCache = false,
            downloadRecordDao = downloadRecordDao
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isCurrentEpisodeDownloaded)
        assertFalse(state.isLoadingPlayback)
        assertEquals("ep-1", state.currentEpisode?.id)

        viewModel.release()
    }

    @Test
    fun vodPlayer_plays_offline_when_network_fails_in_airplane_mode() = runTest {
        val watchHistoryDao = FakeWatchHistoryDao()
        val bookmarkDao = FakeBookmarkDao()
        val downloadRecordDao = FakeDownloadRecordDao()
        val licenseRepository = FakeLicenseRepository(vipActive = true)
        val entitlementManager = EntitlementManager(licenseRepository)

        // Seed completed offline download
        val offlineUri = "file:///data/user/0/com.dramix.app/files/dramix_downloads/ep-1.mp4"
        downloadRecordDao.insertOrUpdateDownload(
            DownloadRecordEntity(
                mediaId = "ep-1",
                dramaId = "drama-101",
                providerId = "wetv",
                dramaTitle = "Love Between Fairy and Devil (Offline)",
                episodeNumber = 1,
                streamUrl = "https://cdn.example.com/stream.m3u8",
                localUri = offlineUri,
                status = "COMPLETED"
            )
        )

        // Repository that always throws network failure (Airplane Mode simulation)
        val failingRepo = object : CatalogRepository {
            override suspend fun getProviders(): Result<List<ProviderModel>> = Result.failure(java.net.UnknownHostException("No connection"))
            override suspend fun getCategories(modelId: String): Result<List<Category>> = Result.failure(java.net.UnknownHostException("No connection"))
            override suspend fun getVideos(modelId: String, categoryId: String, page: Int): Result<List<VideoItem>> = Result.failure(java.net.UnknownHostException("No connection"))
            override suspend fun getDramaDetail(modelId: String, id: String): Result<DramaDetail> = Result.failure(java.net.UnknownHostException("Airplane mode"))
            override suspend fun getPlaybackSource(modelId: String, episodeId: String, id: String?): Result<PlaybackSource> = Result.failure(java.net.UnknownHostException("Airplane mode"))
            override suspend fun search(modelId: String, query: String, page: Int, contentType: String?): Result<List<VideoItem>> = Result.failure(java.net.UnknownHostException("No connection"))
        }

        val viewModel = VodPlayerViewModel(
            providerId = "wetv",
            dramaId = "drama-101",
            catalogRepository = failingRepo,
            watchHistoryDao = watchHistoryDao,
            bookmarkDao = bookmarkDao,
            entitlementManager = entitlementManager,
            licenseRepository = licenseRepository,
            playerFactory = playerFactory,
            enablePlayerCache = false,
            downloadRecordDao = downloadRecordDao
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.detail)
        assertEquals("Love Between Fairy and Devil (Offline)", state.detail?.title)
        assertEquals(1, state.currentEpisode?.number)
        assertTrue(state.isCurrentEpisodeDownloaded)
        assertFalse(state.isLoadingPlayback)

        viewModel.release()
    }
}
