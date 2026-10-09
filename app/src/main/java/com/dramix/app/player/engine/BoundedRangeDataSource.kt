package com.dramix.app.player.engine

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException

/**
 * A DataSource wrapper that limits HTTP range requests to bounded chunk windows
 * (default 10 MB).
 *
 * For progressive media (MP4) which typically issues unbounded requests (length = C.LENGTH_UNSET),
 * this DataSource splits the transfer into demand-driven chunk windows. It fetches data
 * incrementally as the player reads samples, closing and re-opening HTTP range requests seamlessly.
 *
 * This prevents the player from aggressively downloading the entire video file (e.g. 1 GB+)
 * over high-speed networks when the user only watches a portion of the media, saving user data quota
 * and reducing initial buffering latency.
 */
@OptIn(UnstableApi::class)
class BoundedRangeDataSource(
    private val upstreamDataSource: DataSource,
    private val maxChunkSize: Long = DEFAULT_CHUNK_SIZE
) : DataSource {

    companion object {
        const val DEFAULT_CHUNK_SIZE = 10L * 1024 * 1024 // 10 MB per chunk window
    }

    private var originalDataSpec: DataSpec? = null
    private var currentPosition: Long = 0L
    private var currentChunkEndPosition: Long = 0L
    private var totalResourceLength: Long = C.LENGTH_UNSET.toLong()
    private var isChunkingActive: Boolean = false
    private var isOpened: Boolean = false

    override fun addTransferListener(transferListener: TransferListener) {
        upstreamDataSource.addTransferListener(transferListener)
    }

    override fun getUri(): Uri? = upstreamDataSource.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstreamDataSource.responseHeaders

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        originalDataSpec = dataSpec
        currentPosition = dataSpec.position
        isOpened = true

        // If request is already explicitly bounded and smaller than chunk size, pass through directly
        if (dataSpec.length != C.LENGTH_UNSET.toLong() && dataSpec.length <= maxChunkSize) {
            isChunkingActive = false
            return upstreamDataSource.open(dataSpec)
        }

        // Bounded chunking for unbounded or large ranges (e.g. progressive MP4)
        isChunkingActive = true
        val firstChunkLength = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            minOf(maxChunkSize, dataSpec.length)
        } else {
            maxChunkSize
        }

        val subSpec = dataSpec.buildUpon()
            .setPosition(currentPosition)
            .setLength(firstChunkLength)
            .build()

        val bytesOpened = upstreamDataSource.open(subSpec)
        val chunkBytes = if (bytesOpened != C.LENGTH_UNSET.toLong()) bytesOpened else firstChunkLength
        currentChunkEndPosition = currentPosition + chunkBytes

        // Inspect Content-Range response header to determine total resource size
        val headers = upstreamDataSource.responseHeaders
        val contentRange = headers["Content-Range"]?.firstOrNull()
            ?: headers["content-range"]?.firstOrNull()

        if (contentRange != null) {
            val slashIndex = contentRange.lastIndexOf('/')
            if (slashIndex != -1) {
                val totalStr = contentRange.substring(slashIndex + 1).trim()
                val parsedTotal = totalStr.toLongOrNull()
                if (parsedTotal != null && parsedTotal > 0L) {
                    totalResourceLength = parsedTotal
                }
            }
        } else if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
            // Server did not return Content-Range (likely 200 OK without partial range support).
            // Fall back to unbounded stream without chunking to prevent playback breakages.
            isChunkingActive = false
            return bytesOpened
        }

        if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            totalResourceLength = dataSpec.position + dataSpec.length
        }

        return if (totalResourceLength != C.LENGTH_UNSET.toLong()) {
            totalResourceLength - dataSpec.position
        } else {
            C.LENGTH_UNSET.toLong()
        }
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (!isOpened) return C.RESULT_END_OF_INPUT

        if (!isChunkingActive) {
            return upstreamDataSource.read(buffer, offset, length)
        }

        val remainingInChunk = currentChunkEndPosition - currentPosition
        if (remainingInChunk <= 0L) {
            if (totalResourceLength != C.LENGTH_UNSET.toLong() && currentPosition >= totalResourceLength) {
                return C.RESULT_END_OF_INPUT
            }

            val openedNext = openNextChunk()
            if (!openedNext) {
                return C.RESULT_END_OF_INPUT
            }
        }

        val bytesToRead = minOf(length.toLong(), currentChunkEndPosition - currentPosition).toInt()
        val bytesRead = upstreamDataSource.read(buffer, offset, bytesToRead)

        if (bytesRead == C.RESULT_END_OF_INPUT) {
            if (totalResourceLength != C.LENGTH_UNSET.toLong() && currentPosition < totalResourceLength) {
                val openedNext = openNextChunk()
                if (openedNext) {
                    return read(buffer, offset, length)
                }
            }
            return C.RESULT_END_OF_INPUT
        }

        currentPosition += bytesRead
        return bytesRead
    }

    @Throws(IOException::class)
    private fun openNextChunk(): Boolean {
        val spec = originalDataSpec ?: return false
        if (totalResourceLength != C.LENGTH_UNSET.toLong() && currentPosition >= totalResourceLength) {
            return false
        }

        try {
            upstreamDataSource.close()
        } catch (_: IOException) {
            // Ignore close exception on old chunk connection
        }

        val nextChunkLength = if (totalResourceLength != C.LENGTH_UNSET.toLong()) {
            minOf(maxChunkSize, totalResourceLength - currentPosition)
        } else {
            maxChunkSize
        }

        if (nextChunkLength <= 0L) return false

        val nextSpec = spec.buildUpon()
            .setPosition(currentPosition)
            .setLength(nextChunkLength)
            .build()

        val bytesOpened = upstreamDataSource.open(nextSpec)
        val chunkBytes = if (bytesOpened != C.LENGTH_UNSET.toLong()) bytesOpened else nextChunkLength
        currentChunkEndPosition = currentPosition + chunkBytes
        return true
    }

    @Throws(IOException::class)
    override fun close() {
        isOpened = false
        isChunkingActive = false
        originalDataSpec = null
        currentPosition = 0L
        currentChunkEndPosition = 0L
        totalResourceLength = C.LENGTH_UNSET.toLong()
        upstreamDataSource.close()
    }
}
