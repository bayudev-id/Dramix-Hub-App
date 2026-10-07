package com.dramix.app.player.download

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.dramix.app.core.database.dao.DownloadRecordDao
import com.dramix.app.core.database.entity.DownloadRecordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class DownloadTracker(
    private val context: Context,
    private val downloadManager: DownloadManager,
    private val downloadRecordDao: DownloadRecordDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    val allDownloads: Flow<List<DownloadRecordEntity>> = downloadRecordDao.getAllDownloads()

    private var progressPollingJob: Job? = null

    private val downloadListener = object : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            handleDownloadUpdate(download, finalException)
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            val mediaId = download.request.id
            scope.launch {
                downloadRecordDao.deleteDownload(mediaId)
            }
        }
    }

    init {
        downloadManager.addListener(downloadListener)
        startProgressPolling()
    }

    fun startDownload(
        dramaId: String,
        providerId: String,
        dramaTitle: String,
        episodeNumber: Int,
        episodeTitle: String?,
        streamUrl: String,
        mediaId: String = "${dramaId}_${providerId}_$episodeNumber",
        headers: Map<String, String> = emptyMap()
    ) {
        scope.launch {
            // 1. Record QUEUED state in Room DB
            val record = DownloadRecordEntity(
                mediaId = mediaId,
                dramaId = dramaId,
                providerId = providerId,
                dramaTitle = dramaTitle,
                episodeNumber = episodeNumber,
                episodeTitle = episodeTitle,
                streamUrl = streamUrl,
                status = "QUEUED",
                bytesDownloaded = 0L,
                totalBytes = 0L,
                progressPercentage = 0,
                createdAt = System.currentTimeMillis()
            )
            downloadRecordDao.insertOrUpdateDownload(record)

            // 2. Build Media3 DownloadRequest
            val uri = Uri.parse(streamUrl)
            val request = DownloadRequest.Builder(mediaId, uri).build()

            // 3. Send command to Media3 DownloadService and DownloadManager
            try {
                DownloadService.sendAddDownload(
                    context,
                    DramixDownloadService::class.java,
                    request,
                    false
                )
            } catch (_: Exception) {
                // Fallback for background thread or test environments
            }
            downloadManager.addDownload(request)
        }
    }

    fun pauseDownload(mediaId: String) {
        scope.launch {
            try {
                DownloadService.sendSetStopReason(
                    context,
                    DramixDownloadService::class.java,
                    mediaId,
                    Download.STOP_REASON_NONE + 1,
                    false
                )
            } catch (_: Exception) {
            }
            downloadManager.setStopReason(mediaId, Download.STOP_REASON_NONE + 1)
            downloadRecordDao.updateDownloadStatus(mediaId, "PAUSED")
        }
    }

    fun resumeDownload(mediaId: String) {
        scope.launch {
            try {
                DownloadService.sendSetStopReason(
                    context,
                    DramixDownloadService::class.java,
                    mediaId,
                    Download.STOP_REASON_NONE,
                    false
                )
            } catch (_: Exception) {
            }
            downloadManager.setStopReason(mediaId, Download.STOP_REASON_NONE)
            downloadRecordDao.updateDownloadStatus(mediaId, "DOWNLOADING")
        }
    }

    fun removeDownload(mediaId: String) {
        scope.launch {
            try {
                DownloadService.sendRemoveDownload(
                    context,
                    DramixDownloadService::class.java,
                    mediaId,
                    false
                )
            } catch (_: Exception) {
            }
            downloadManager.removeDownload(mediaId)
            downloadRecordDao.deleteDownload(mediaId)
        }
    }

    suspend fun getDownloadedRecord(mediaId: String): DownloadRecordEntity? {
        return downloadRecordDao.getDownloadByMediaIdSync(mediaId)
    }

    suspend fun isDownloaded(mediaId: String): Boolean {
        val record = downloadRecordDao.getDownloadByMediaIdSync(mediaId)
        return record != null && record.status == "COMPLETED" && !record.localUri.isNullOrBlank()
    }

    internal fun handleDownloadUpdate(download: Download, finalException: Exception?) {
        val mediaId = download.request.id
        val percent = download.percentDownloaded
        val bytes = download.bytesDownloaded
        val total = download.contentLength

        scope.launch {
            when (download.state) {
                Download.STATE_QUEUED -> {
                    downloadRecordDao.updateDownloadProgress(
                        mediaId = mediaId,
                        bytesDownloaded = bytes,
                        totalBytes = if (total > 0) total else 0L,
                        progressPercentage = 0,
                        status = "QUEUED"
                    )
                }

                Download.STATE_DOWNLOADING -> {
                    val progress = if (percent >= 0f) percent.toInt().coerceIn(0, 99) else 0
                    downloadRecordDao.updateDownloadProgress(
                        mediaId = mediaId,
                        bytesDownloaded = bytes,
                        totalBytes = if (total > 0) total else 0L,
                        progressPercentage = progress,
                        status = "DOWNLOADING"
                    )
                }

                Download.STATE_COMPLETED -> {
                    val localUri = download.request.uri.toString()
                    downloadRecordDao.updateDownloadCompleted(
                        mediaId = mediaId,
                        localUri = localUri,
                        status = "COMPLETED",
                        completedAt = System.currentTimeMillis()
                    )
                }

                Download.STATE_FAILED -> {
                    val errMsg = finalException?.localizedMessage ?: "Unduhan gagal"
                    downloadRecordDao.updateDownloadStatus(
                        mediaId = mediaId,
                        status = "FAILED",
                        errorMessage = errMsg
                    )
                }

                Download.STATE_STOPPED -> {
                    downloadRecordDao.updateDownloadStatus(
                        mediaId = mediaId,
                        status = "PAUSED"
                    )
                }

                Download.STATE_REMOVING -> {
                    downloadRecordDao.deleteDownload(mediaId)
                }
            }
        }
    }

    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val currentList = downloadManager.currentDownloads
                for (download in currentList) {
                    if (download.state == Download.STATE_DOWNLOADING) {
                        val mediaId = download.request.id
                        val percent = download.percentDownloaded
                        val bytes = download.bytesDownloaded
                        val total = download.contentLength
                        val progress = if (percent >= 0f) percent.toInt().coerceIn(0, 99) else 0

                        downloadRecordDao.updateDownloadProgress(
                            mediaId = mediaId,
                            bytesDownloaded = bytes,
                            totalBytes = if (total > 0) total else 0L,
                            progressPercentage = progress,
                            status = "DOWNLOADING"
                        )
                    }
                }
            }
        }
    }

    fun release() {
        progressPollingJob?.cancel()
        downloadManager.removeListener(downloadListener)
    }
}
