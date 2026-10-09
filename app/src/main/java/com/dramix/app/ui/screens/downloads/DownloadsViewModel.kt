package com.dramix.app.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dramix.app.core.database.dao.DownloadRecordDao
import com.dramix.app.core.database.entity.DownloadRecordEntity
import com.dramix.app.player.download.DownloadTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DownloadFilterTab(val label: String) {
    ALL("Semua"),
    COMPLETED("Selesai"),
    DOWNLOADING("Mengunduh")
}

data class DownloadsUiState(
    val allDownloads: List<DownloadRecordEntity> = emptyList(),
    val filteredDownloads: List<DownloadRecordEntity> = emptyList(),
    val selectedFilter: DownloadFilterTab = DownloadFilterTab.ALL,
    val totalDownloadedBytes: Long = 0L,
    val completedCount: Int = 0,
    val downloadingCount: Int = 0
)

class DownloadsViewModel(
    private val downloadRecordDao: DownloadRecordDao,
    private val downloadTracker: DownloadTracker? = null
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(DownloadFilterTab.ALL)
    val selectedFilter: StateFlow<DownloadFilterTab> = _selectedFilter.asStateFlow()

    private val _rawDownloads = downloadRecordDao.getAllDownloads()

    val uiState: StateFlow<DownloadsUiState> = combine(
        _rawDownloads,
        _selectedFilter
    ) { downloads, filter ->
        val completed = downloads.filter { it.status.equals("COMPLETED", ignoreCase = true) }
        val active = downloads.filter {
            val s = it.status.uppercase()
            s == "DOWNLOADING" || s == "QUEUED" || s == "PAUSED"
        }

        val filtered = when (filter) {
            DownloadFilterTab.ALL -> downloads
            DownloadFilterTab.COMPLETED -> completed
            DownloadFilterTab.DOWNLOADING -> active
        }

        val totalBytes = completed.sumOf { it.totalBytes.coerceAtLeast(it.bytesDownloaded) }

        DownloadsUiState(
            allDownloads = downloads,
            filteredDownloads = filtered,
            selectedFilter = filter,
            totalDownloadedBytes = totalBytes,
            completedCount = completed.size,
            downloadingCount = active.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DownloadsUiState()
    )

    fun setFilter(filter: DownloadFilterTab) {
        _selectedFilter.value = filter
    }

    fun pauseDownload(record: DownloadRecordEntity) {
        viewModelScope.launch {
            if (downloadTracker != null) {
                downloadTracker.pauseDownload(record.mediaId)
            } else {
                downloadRecordDao.updateDownloadStatus(record.mediaId, "PAUSED")
            }
        }
    }

    fun resumeDownload(record: DownloadRecordEntity) {
        viewModelScope.launch {
            if (downloadTracker != null) {
                downloadTracker.resumeDownload(record.mediaId)
            } else {
                downloadRecordDao.updateDownloadStatus(record.mediaId, "DOWNLOADING")
            }
        }
    }

    fun deleteDownload(record: DownloadRecordEntity) {
        viewModelScope.launch {
            if (downloadTracker != null) {
                downloadTracker.removeDownload(record.mediaId)
            } else {
                downloadRecordDao.deleteDownload(record.mediaId)
            }
        }
    }

    fun retryDownload(record: DownloadRecordEntity) {
        viewModelScope.launch {
            downloadTracker?.resumeDownload(record.mediaId)
            downloadRecordDao.updateDownloadStatus(record.mediaId, "DOWNLOADING")
        }
    }
}
