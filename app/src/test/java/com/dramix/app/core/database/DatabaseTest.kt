package com.dramix.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dramix.app.core.database.dao.BookmarkDao
import com.dramix.app.core.database.dao.DownloadRecordDao
import com.dramix.app.core.database.dao.WatchHistoryDao
import com.dramix.app.core.database.entity.BookmarkEntity
import com.dramix.app.core.database.entity.DownloadRecordEntity
import com.dramix.app.core.database.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class DatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var watchHistoryDao: WatchHistoryDao
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var downloadRecordDao: DownloadRecordDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        watchHistoryDao = db.watchHistoryDao()
        bookmarkDao = db.bookmarkDao()
        downloadRecordDao = db.downloadRecordDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun watchHistory_upsert_and_retrieve_latest_drama() = runTest {
        val dramaId = "drama-101"
        val providerId = "wetv"

        // Watch episode 1
        val ep1 = WatchHistoryEntity(
            dramaId = dramaId,
            providerId = providerId,
            dramaTitle = "Love Between Fairy and Devil",
            episodeNumber = 1,
            positionMs = 120_000L,
            durationMs = 2_700_000L,
            updatedAt = 1_000L
        )
        watchHistoryDao.insertOrUpdateWatchHistory(ep1)

        // Later watch episode 2
        val ep2 = WatchHistoryEntity(
            dramaId = dramaId,
            providerId = providerId,
            dramaTitle = "Love Between Fairy and Devil",
            episodeNumber = 2,
            positionMs = 50_000L,
            durationMs = 2_700_000L,
            updatedAt = 2_000L
        )
        watchHistoryDao.insertOrUpdateWatchHistory(ep2)

        // Episode history should contain 2 records
        val allEpisodes = watchHistoryDao.getDramaHistory(dramaId, providerId).first()
        assertEquals(2, allEpisodes.size)

        // Home feed 'getLatestWatchedDramas' must return exactly 1 item for this drama, pointing to episode 2
        val latestDramas = watchHistoryDao.getLatestWatchedDramas().first()
        assertEquals(1, latestDramas.size)
        assertEquals(2, latestDramas.first().episodeNumber)
        assertEquals(50_000L, latestDramas.first().positionMs)
    }

    @Test
    fun bookmark_insert_and_toggle() = runTest {
        val dramaId = "drama-202"
        val providerId = "cineflow"

        // Initial state: not bookmarked
        assertFalse(bookmarkDao.isBookmarked(dramaId, providerId).first())

        // Insert bookmark
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                dramaId = dramaId,
                providerId = providerId,
                title = "Hidden Love",
                contentType = "long_drama"
            )
        )

        // Bookmarked state: true
        assertTrue(bookmarkDao.isBookmarked(dramaId, providerId).first())

        // Delete bookmark
        bookmarkDao.deleteBookmark(dramaId, providerId)

        // Bookmarked state: false
        assertFalse(bookmarkDao.isBookmarked(dramaId, providerId).first())
    }

    @Test
    fun downloadRecord_crud_and_status_update() = runTest {
        val mediaId = "cineflow:drama-202:ep-1"
        val record = DownloadRecordEntity(
            mediaId = mediaId,
            dramaId = "drama-202",
            providerId = "cineflow",
            dramaTitle = "Hidden Love",
            episodeNumber = 1,
            streamUrl = "https://example.com/stream.m3u8",
            status = "QUEUED"
        )

        downloadRecordDao.insertOrUpdateDownload(record)

        val retrieved = downloadRecordDao.getDownloadByMediaIdSync(mediaId)
        assertNotNull(retrieved)
        assertEquals("QUEUED", retrieved?.status)

        // Update progress
        downloadRecordDao.updateDownloadProgress(
            mediaId = mediaId,
            bytesDownloaded = 50_000_000L,
            totalBytes = 100_000_000L,
            progressPercentage = 50,
            status = "DOWNLOADING"
        )

        val inProgress = downloadRecordDao.getDownloadByMediaIdSync(mediaId)
        assertEquals("DOWNLOADING", inProgress?.status)
        assertEquals(50, inProgress?.progressPercentage)
        assertEquals(50_000_000L, inProgress?.bytesDownloaded)
    }
}
