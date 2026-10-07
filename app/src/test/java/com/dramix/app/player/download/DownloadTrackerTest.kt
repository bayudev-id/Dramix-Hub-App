package com.dramix.app.player.download

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.test.core.app.ApplicationProvider
import com.dramix.app.core.database.dao.DownloadRecordDao
import com.dramix.app.core.database.entity.DownloadRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(UnstableApi::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DownloadTrackerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var context: Context

    private class FakeDownloadRecordDao : DownloadRecordDao {
        val records = mutableMapOf<String, DownloadRecordEntity>()
        val recordsFlow = MutableStateFlow<List<DownloadRecordEntity>>(emptyList())

        private fun updateFlow() {
            recordsFlow.value = records.values.sortedByDescending { it.createdAt }
        }

        override suspend fun insertOrUpdateDownload(record: DownloadRecordEntity): Long {
            records[record.mediaId] = record
            updateFlow()
            return 1L
        }

        override suspend fun getDownloadByMediaIdSync(mediaId: String): DownloadRecordEntity? {
            return records[mediaId]
        }

        override fun getDownloadByMediaId(mediaId: String): Flow<DownloadRecordEntity?> {
            return recordsFlow.map { list -> list.find { it.mediaId == mediaId } }
        }

        override fun getAllDownloads(): Flow<List<DownloadRecordEntity>> = recordsFlow

        override fun getDownloadsByStatus(status: String): Flow<List<DownloadRecordEntity>> {
            return recordsFlow.map { list -> list.filter { it.status.equals(status, ignoreCase = true) } }
        }

        override suspend fun updateDownloadProgress(
            mediaId: String,
            bytesDownloaded: Long,
            totalBytes: Long,
            progressPercentage: Int,
            status: String
        ): Int {
            val existing = records[mediaId] ?: return 0
            records[mediaId] = existing.copy(
                bytesDownloaded = bytesDownloaded,
                totalBytes = totalBytes,
                progressPercentage = progressPercentage,
                status = status
            )
            updateFlow()
            return 1
        }

        override suspend fun updateDownloadStatus(
            mediaId: String,
            status: String,
            completedAt: Long?,
            errorMessage: String?
        ): Int {
            val existing = records[mediaId] ?: return 0
            records[mediaId] = existing.copy(
                status = status,
                completedAt = completedAt,
                errorMessage = errorMessage
            )
            updateFlow()
            return 1
        }

        override suspend fun updateDownloadCompleted(
            mediaId: String,
            localUri: String,
            status: String,
            completedAt: Long
        ): Int {
            val existing = records[mediaId] ?: return 0
            records[mediaId] = existing.copy(
                status = status,
                localUri = localUri,
                progressPercentage = 100,
                completedAt = completedAt,
                errorMessage = null
            )
            updateFlow()
            return 1
        }

        override suspend fun deleteDownload(mediaId: String): Int {
            val removed = records.remove(mediaId) != null
            if (removed) updateFlow()
            return if (removed) 1 else 0
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun downloadManagerHelper_injects_valid_referer_and_user_agent_headers() {
        val client = OkHttpClient()
        val factory = DownloadManagerHelper.getHttpDataSourceFactory(client)
        val headers = factory.getHeaders()

        assertTrue(headers.containsKey("User-Agent"))
        assertTrue(headers["User-Agent"]?.contains("Dramix") == true)
        assertTrue(headers.containsKey("Referer"))
        assertEquals("https://dramix.app/", headers["Referer"])
    }

    @Test
    fun startDownload_inserts_queued_record_and_builds_request() = runTest {
        val dao = FakeDownloadRecordDao()
        val downloadManager = DownloadManagerHelper.getDownloadManager(context, OkHttpClient())
        val tracker = DownloadTracker(context, downloadManager, dao, scope = this)

        tracker.startDownload(
            dramaId = "drama-101",
            providerId = "wetv",
            dramaTitle = "Love Between Fairy and Devil",
            episodeNumber = 1,
            episodeTitle = "Pertemuan Takdir",
            streamUrl = "https://cdn.example.com/hls/ep1.m3u8",
            mediaId = "drama-101_wetv_1"
        )

        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()

        val record = dao.getDownloadByMediaIdSync("drama-101_wetv_1")
        assertNotNull(record)
        assertEquals("QUEUED", record?.status)
        assertEquals(0, record?.progressPercentage)
        assertEquals("Love Between Fairy and Devil", record?.dramaTitle)
        assertEquals("https://cdn.example.com/hls/ep1.m3u8", record?.streamUrl)

        tracker.release()
    }

    @Test
    fun download_lifecycle_transitions_update_room_database() = runTest {
        val dao = FakeDownloadRecordDao()
        val downloadManager = DownloadManagerHelper.getDownloadManager(context, OkHttpClient())
        val tracker = DownloadTracker(context, downloadManager, dao, scope = this)

        val mediaId = "drama-200_wetv_2"
        tracker.startDownload(
            dramaId = "drama-200",
            providerId = "wetv",
            dramaTitle = "Hidden Love",
            episodeNumber = 2,
            episodeTitle = "Episode 2",
            streamUrl = "https://cdn.example.com/hls/ep2.m3u8",
            mediaId = mediaId
        )

        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()

        // 1. Simulate STATE_DOWNLOADING (45%)
        val request = DownloadRequest.Builder(mediaId, Uri.parse("https://cdn.example.com/hls/ep2.m3u8")).build()
        val downloadingDownload = Download(
            request,
            Download.STATE_DOWNLOADING,
            /* startTimeMs = */ 1000L,
            /* updateTimeMs = */ 2000L,
            /* contentLength = */ 100_000_000L,
            /* stopReason = */ 0,
            /* failureReason = */ 0
        )
        // Set progress bytes via progress simulation
        tracker.handleDownloadUpdate(downloadingDownload, null)
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()

        var record = dao.getDownloadByMediaIdSync(mediaId)
        assertEquals("DOWNLOADING", record?.status)

        // 2. Simulate STATE_COMPLETED
        val completedDownload = Download(
            request,
            Download.STATE_COMPLETED,
            /* startTimeMs = */ 1000L,
            /* updateTimeMs = */ 5000L,
            /* contentLength = */ 100_000_000L,
            /* stopReason = */ 0,
            /* failureReason = */ 0
        )
        tracker.handleDownloadUpdate(completedDownload, null)
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()

        record = dao.getDownloadByMediaIdSync(mediaId)
        assertEquals("COMPLETED", record?.status)
        assertEquals(100, record?.progressPercentage)
        assertEquals("https://cdn.example.com/hls/ep2.m3u8", record?.localUri)
        assertNotNull(record?.completedAt)
        assertNull(record?.errorMessage)

        assertTrue(tracker.isDownloaded(mediaId))

        // 3. Simulate STATE_FAILED on error
        val failedDownload = Download(
            request,
            Download.STATE_FAILED,
            /* startTimeMs = */ 1000L,
            /* updateTimeMs = */ 6000L,
            /* contentLength = */ 100_000_000L,
            /* stopReason = */ 0,
            /* failureReason = */ Download.FAILURE_REASON_UNKNOWN
        )
        tracker.handleDownloadUpdate(failedDownload, IllegalStateException("Network timeout"))
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()

        record = dao.getDownloadByMediaIdSync(mediaId)
        assertEquals("FAILED", record?.status)
        assertEquals("Network timeout", record?.errorMessage)

        tracker.release()
    }

    @Test
    fun pause_resume_and_remove_download_controls() = runTest {
        val dao = FakeDownloadRecordDao()
        val downloadManager = DownloadManagerHelper.getDownloadManager(context, OkHttpClient())
        val tracker = DownloadTracker(context, downloadManager, dao, scope = this)

        val mediaId = "drama-300_wetv_3"
        tracker.startDownload(
            dramaId = "drama-300",
            providerId = "wetv",
            dramaTitle = "Eternal Love",
            episodeNumber = 3,
            episodeTitle = "Episode 3",
            streamUrl = "https://cdn.example.com/hls/ep3.m3u8",
            mediaId = mediaId
        )

        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()

        // Pause
        tracker.pauseDownload(mediaId)
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()

        var record = dao.getDownloadByMediaIdSync(mediaId)
        assertEquals("PAUSED", record?.status)

        // Resume
        tracker.resumeDownload(mediaId)
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()

        record = dao.getDownloadByMediaIdSync(mediaId)
        assertEquals("DOWNLOADING", record?.status)

        // Remove
        tracker.removeDownload(mediaId)
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()

        record = dao.getDownloadByMediaIdSync(mediaId)
        assertNull(record)

        tracker.release()
    }
}
