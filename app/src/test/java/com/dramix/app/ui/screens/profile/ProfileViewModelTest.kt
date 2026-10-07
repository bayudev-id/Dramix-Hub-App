package com.dramix.app.ui.screens.profile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dramix.app.core.database.dao.BookmarkDao
import com.dramix.app.core.database.dao.DownloadRecordDao
import com.dramix.app.core.database.dao.WatchHistoryDao
import com.dramix.app.core.database.entity.BookmarkEntity
import com.dramix.app.core.database.entity.DownloadRecordEntity
import com.dramix.app.core.database.entity.WatchHistoryEntity
import com.dramix.app.core.security.DeviceIdentifier
import com.dramix.app.data.source.local.LicensePreferences
import com.dramix.app.domain.model.LicenseStatus
import com.dramix.app.domain.repository.LicenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var licensePreferences: LicensePreferences
    private lateinit var deviceIdentifier: DeviceIdentifier

    private class FakeLicenseRepository(initialVip: Boolean = false) : LicenseRepository {
        val vipFlow = MutableStateFlow(initialVip)
        var shouldFail = false

        override suspend fun activateLicense(licenseKey: String): Result<LicenseStatus> {
            if (shouldFail) {
                return Result.failure(RuntimeException("Lisensi sudah terikat ke HP lain"))
            }
            vipFlow.value = true
            return Result.success(
                LicenseStatus(
                    isVip = true,
                    licenseKey = licenseKey,
                    token = "token-xyz",
                    expiresAt = System.currentTimeMillis() + 86400000L * 30,
                    planName = "VIP Bulanan"
                )
            )
        }

        override suspend fun refreshLicenseStatus(): Result<LicenseStatus> = Result.success(
            LicenseStatus(isVip = vipFlow.value)
        )

        override fun isVipActive(): Boolean = vipFlow.value
        override fun getSessionToken(): String? = if (vipFlow.value) "token" else null
        override fun observeVipStatus(): Flow<Boolean> = vipFlow
        override fun clearLicense() { vipFlow.value = false }
    }

    private class FakeWatchHistoryDao : WatchHistoryDao {
        val list = MutableStateFlow<List<WatchHistoryEntity>>(
            listOf(
                WatchHistoryEntity(
                    id = 1L,
                    dramaId = "d-1",
                    providerId = "wetv",
                    dramaTitle = "Love Drama",
                    episodeNumber = 1,
                    positionMs = 60000L,
                    durationMs = 2400000L,
                    isCompleted = false,
                    updatedAt = System.currentTimeMillis()
                )
            )
        )

        override suspend fun insertOrUpdateWatchHistory(history: WatchHistoryEntity): Long = 1L
        override suspend fun getEpisodeHistory(dramaId: String, providerId: String, episodeNumber: Int): WatchHistoryEntity? = null
        override fun getDramaHistory(dramaId: String, providerId: String): Flow<List<WatchHistoryEntity>> = list
        override fun getLatestWatchedDramas(limit: Int): Flow<List<WatchHistoryEntity>> = list
        override suspend fun deleteDramaHistory(dramaId: String, providerId: String): Int = 1
        override suspend fun clearAllHistory(): Int {
            list.value = emptyList()
            return 1
        }
    }

    private class FakeBookmarkDao : BookmarkDao {
        val list = MutableStateFlow<List<BookmarkEntity>>(
            listOf(
                BookmarkEntity(
                    id = 1L,
                    dramaId = "d-1",
                    providerId = "wetv",
                    title = "Love Drama",
                    rating = "9.5",
                    totalEpisodes = 24
                )
            )
        )

        override suspend fun insertBookmark(bookmark: BookmarkEntity): Long = 1L
        override suspend fun deleteBookmark(dramaId: String, providerId: String): Int {
            list.value = list.value.filterNot { it.dramaId == dramaId && it.providerId == providerId }
            return 1
        }
        override fun getAllBookmarks(): Flow<List<BookmarkEntity>> = list
        override fun isBookmarked(dramaId: String, providerId: String): Flow<Boolean> = flowOf(true)
        override suspend fun getBookmark(dramaId: String, providerId: String): BookmarkEntity? = null
    }

    private class FakeDownloadRecordDao : DownloadRecordDao {
        val list = MutableStateFlow<List<DownloadRecordEntity>>(
            listOf(
                DownloadRecordEntity(
                    id = 1L,
                    mediaId = "ep-1",
                    dramaId = "d-1",
                    providerId = "wetv",
                    dramaTitle = "Love Drama",
                    episodeNumber = 1,
                    episodeTitle = "Episode 1",
                    streamUrl = "https://example.com/stream.mp4",
                    localUri = "/storage/ep-1.mp4",
                    status = "completed",
                    progressPercentage = 100
                )
            )
        )

        override suspend fun insertOrUpdateDownload(record: DownloadRecordEntity): Long = 1L
        override suspend fun getDownloadByMediaIdSync(mediaId: String): DownloadRecordEntity? = null
        override fun getDownloadByMediaId(mediaId: String): Flow<DownloadRecordEntity?> = flowOf(null)
        override fun getAllDownloads(): Flow<List<DownloadRecordEntity>> = list
        override fun getDownloadsByStatus(status: String): Flow<List<DownloadRecordEntity>> = list
        override suspend fun updateDownloadProgress(mediaId: String, bytesDownloaded: Long, totalBytes: Long, progressPercentage: Int, status: String): Int = 1
        override suspend fun updateDownloadStatus(mediaId: String, status: String, completedAt: Long?, errorMessage: String?): Int = 1
        override suspend fun updateDownloadCompleted(mediaId: String, localUri: String, status: String, completedAt: Long): Int = 1
        override suspend fun deleteDownload(mediaId: String): Int = 1
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        licensePreferences = LicensePreferences(context)
        deviceIdentifier = DeviceIdentifier(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun profile_initializes_with_device_id_and_free_status() = runTest {
        val repo = FakeLicenseRepository(initialVip = false)
        val watchDao = FakeWatchHistoryDao()
        val bookmarkDao = FakeBookmarkDao()
        val downloadDao = FakeDownloadRecordDao()

        val viewModel = ProfileViewModel(
            licenseRepository = repo,
            licensePreferences = licensePreferences,
            deviceIdentifier = deviceIdentifier,
            watchHistoryDao = watchDao,
            bookmarkDao = bookmarkDao,
            downloadRecordDao = downloadDao,
            context = context
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.deviceIdFormatted.startsWith("DRM-"))
        assertTrue(state.userDisplayName.startsWith("Dramix User #"))
        assertFalse(state.isVip)
        assertEquals(1, state.watchHistory.size)
        assertEquals(1, state.bookmarks.size)
        assertEquals(1, state.downloads.size)
        assertNotNull(state.cacheSizeMb)
    }

    @Test
    fun profile_activates_license_successfully() = runTest {
        val repo = FakeLicenseRepository(initialVip = false)
        val watchDao = FakeWatchHistoryDao()
        val bookmarkDao = FakeBookmarkDao()
        val downloadDao = FakeDownloadRecordDao()

        val viewModel = ProfileViewModel(
            licenseRepository = repo,
            licensePreferences = licensePreferences,
            deviceIdentifier = deviceIdentifier,
            watchHistoryDao = watchDao,
            bookmarkDao = bookmarkDao,
            downloadRecordDao = downloadDao,
            context = context
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isVip)

        viewModel.onLicenseKeyChange("LCN-VIP-MONTHLY")
        assertEquals("LCN-VIP-MONTHLY", viewModel.uiState.value.licenseKeyInput)

        viewModel.activateLicense()

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isVip)
        assertEquals("Lisensi VIP Berhasil Diaktifkan!", state.activationSuccessMessage)
        assertEquals("", state.licenseKeyInput)
    }

    @Test
    fun profile_handles_activation_error() = runTest {
        val repo = FakeLicenseRepository(initialVip = false).apply { shouldFail = true }
        val watchDao = FakeWatchHistoryDao()
        val bookmarkDao = FakeBookmarkDao()
        val downloadDao = FakeDownloadRecordDao()

        val viewModel = ProfileViewModel(
            licenseRepository = repo,
            licensePreferences = licensePreferences,
            deviceIdentifier = deviceIdentifier,
            watchHistoryDao = watchDao,
            bookmarkDao = bookmarkDao,
            downloadRecordDao = downloadDao,
            context = context
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        viewModel.onLicenseKeyChange("LCN-ALREADY-BOUND")
        viewModel.activateLicense()

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isVip)
        assertEquals("Lisensi sudah terikat ke HP lain", state.activationErrorMessage)
    }

    @Test
    fun profile_clears_cache_and_modifies_library() = runTest {
        val repo = FakeLicenseRepository(initialVip = false)
        val watchDao = FakeWatchHistoryDao()
        val bookmarkDao = FakeBookmarkDao()
        val downloadDao = FakeDownloadRecordDao()

        val viewModel = ProfileViewModel(
            licenseRepository = repo,
            licensePreferences = licensePreferences,
            deviceIdentifier = deviceIdentifier,
            watchHistoryDao = watchDao,
            bookmarkDao = bookmarkDao,
            downloadRecordDao = downloadDao,
            context = context
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        viewModel.openClearCacheDialog()
        assertTrue(viewModel.uiState.value.showClearCacheDialog)

        viewModel.clearCache()
        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.showClearCacheDialog)
        assertEquals("0.0 MB", viewModel.uiState.value.cacheSizeMb)

        // Delete bookmark
        val bookmark = viewModel.uiState.value.bookmarks.first()
        viewModel.deleteBookmark(bookmark)

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.bookmarks.isEmpty())

        // Clear history
        viewModel.clearAllWatchHistory()
        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.watchHistory.isEmpty())
    }
}
