package com.dramix.app.player.engine

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@OptIn(UnstableApi::class)
class BoundedRangeDataSourceTest {

    private class FakeRangeServerDataSource(
        private val totalSize: Int = 25 * 1024 * 1024, // 25 MB
        private val supportRange: Boolean = true
    ) : DataSource {

        val data = ByteArray(totalSize) { (it % 127).toByte() }
        val openCalls = mutableListOf<DataSpec>()
        var isClosed = false
        var currentPosition: Long = 0L
        var bytesRemainingInCurrentOpen: Long = 0L

        private val headers = mutableMapOf<String, List<String>>()

        override fun addTransferListener(transferListener: TransferListener) {}

        override fun getUri(): Uri? = Uri.parse("https://cdn.example.com/video.mp4")

        override fun getResponseHeaders(): Map<String, List<String>> = headers

        override fun open(dataSpec: DataSpec): Long {
            openCalls.add(dataSpec)
            isClosed = false
            currentPosition = dataSpec.position

            if (supportRange) {
                val endPos = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                    currentPosition + dataSpec.length - 1
                } else {
                    totalSize - 1L
                }
                headers["Content-Range"] = listOf("bytes $currentPosition-$endPos/$totalSize")
                bytesRemainingInCurrentOpen = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                    dataSpec.length
                } else {
                    totalSize - currentPosition
                }
                return bytesRemainingInCurrentOpen
            } else {
                headers.clear()
                bytesRemainingInCurrentOpen = totalSize - currentPosition
                return bytesRemainingInCurrentOpen
            }
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (bytesRemainingInCurrentOpen <= 0L) return C.RESULT_END_OF_INPUT
            val toRead = minOf(length.toLong(), bytesRemainingInCurrentOpen).toInt()
            System.arraycopy(data, currentPosition.toInt(), buffer, offset, toRead)
            currentPosition += toRead
            bytesRemainingInCurrentOpen -= toRead
            return toRead
        }

        override fun close() {
            isClosed = true
        }
    }

    private lateinit var fakeServer: FakeRangeServerDataSource
    private val chunkSize = 5 * 1024 * 1024L // 5 MB for unit test

    @Before
    fun setup() {
        fakeServer = FakeRangeServerDataSource(totalSize = 12 * 1024 * 1024) // 12 MB
    }

    @Test
    fun open_unbounded_request_bounds_initial_chunk() {
        val boundedSource = BoundedRangeDataSource(fakeServer, maxChunkSize = chunkSize)
        val dataSpec = DataSpec(Uri.parse("https://cdn.example.com/video.mp4"))

        val totalBytes = boundedSource.open(dataSpec)

        // Total reported should be total resource size (12 MB)
        assertEquals(12 * 1024 * 1024L, totalBytes)
        // Upstream should have received a bounded 5 MB request
        assertEquals(1, fakeServer.openCalls.size)
        assertEquals(0L, fakeServer.openCalls[0].position)
        assertEquals(chunkSize, fakeServer.openCalls[0].length)
    }

    @Test
    fun read_seamlessly_crosses_chunk_boundaries() {
        val boundedSource = BoundedRangeDataSource(fakeServer, maxChunkSize = chunkSize)
        val dataSpec = DataSpec(Uri.parse("https://cdn.example.com/video.mp4"))
        boundedSource.open(dataSpec)

        // Read 7 MB total (crosses the 5 MB chunk 1 into chunk 2)
        val totalToRead = 7 * 1024 * 1024
        val receivedData = ByteArray(totalToRead)
        var totalRead = 0

        val buffer = ByteArray(64 * 1024)
        while (totalRead < totalToRead) {
            val toRead = minOf(buffer.size, totalToRead - totalRead)
            val readCount = boundedSource.read(buffer, 0, toRead)
            if (readCount == C.RESULT_END_OF_INPUT) break
            System.arraycopy(buffer, 0, receivedData, totalRead, readCount)
            totalRead += readCount
        }

        assertEquals(totalToRead, totalRead)
        // Verify byte accuracy
        val expectedData = ByteArray(totalToRead)
        System.arraycopy(fakeServer.data, 0, expectedData, 0, totalToRead)
        assertArrayEquals(expectedData, receivedData)

        // Upstream should have been called twice: 1st chunk (0 to 5MB), 2nd chunk (5MB to 10MB)
        assertEquals(2, fakeServer.openCalls.size)
        assertEquals(0L, fakeServer.openCalls[0].position)
        assertEquals(chunkSize, fakeServer.openCalls[0].length)
        assertEquals(chunkSize, fakeServer.openCalls[1].position)
        assertEquals(chunkSize, fakeServer.openCalls[1].length)
    }

    @Test
    fun small_bounded_requests_pass_through_without_chunking() {
        val boundedSource = BoundedRangeDataSource(fakeServer, maxChunkSize = chunkSize)
        val smallSpec = DataSpec.Builder()
            .setUri(Uri.parse("https://cdn.example.com/video.mp4"))
            .setPosition(100L)
            .setLength(1024L)
            .build()

        val opened = boundedSource.open(smallSpec)
        assertEquals(1024L, opened)
        assertEquals(1, fakeServer.openCalls.size)
        assertEquals(100L, fakeServer.openCalls[0].position)
        assertEquals(1024L, fakeServer.openCalls[0].length)
    }

    @Test
    fun fallback_gracefully_when_server_lacks_range_support() {
        val noRangeServer = FakeRangeServerDataSource(totalSize = 2 * 1024 * 1024, supportRange = false)
        val boundedSource = BoundedRangeDataSource(noRangeServer, maxChunkSize = chunkSize)
        val unboundedSpec = DataSpec(Uri.parse("https://cdn.example.com/video.mp4"))

        val opened = boundedSource.open(unboundedSpec)
        assertEquals(2 * 1024 * 1024L, opened)

        val buffer = ByteArray(1024)
        val read = boundedSource.read(buffer, 0, buffer.size)
        assertTrue(read > 0)
    }

    @Test
    fun close_aborts_upstream_immediately() {
        val boundedSource = BoundedRangeDataSource(fakeServer, maxChunkSize = chunkSize)
        val dataSpec = DataSpec(Uri.parse("https://cdn.example.com/video.mp4"))
        boundedSource.open(dataSpec)
        assertFalse(fakeServer.isClosed)

        boundedSource.close()
        assertTrue(fakeServer.isClosed)
    }
}
