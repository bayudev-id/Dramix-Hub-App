package com.dramix.app.ui.screens.player_vod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dramix.app.core.database.dao.BookmarkDao
import com.dramix.app.core.database.dao.DownloadRecordDao
import com.dramix.app.core.database.dao.WatchHistoryDao
import com.dramix.app.core.database.entity.BookmarkEntity
import com.dramix.app.core.database.entity.WatchHistoryEntity
import com.dramix.app.domain.manager.EntitlementManager
import com.dramix.app.domain.manager.PlaybackAccess
import com.dramix.app.domain.model.DramaDetail
import com.dramix.app.domain.model.Episode
import com.dramix.app.domain.model.PlaybackSource
import com.dramix.app.domain.model.Season
import com.dramix.app.domain.repository.CatalogRepository
import com.dramix.app.domain.repository.LicenseRepository
import com.dramix.app.player.controller.DramixPlayerController
import com.dramix.app.player.download.DownloadTracker
import com.dramix.app.player.engine.PlayerFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class VodPlayerUiState(
    val isLoadingDetail: Boolean = true,
    val isLoadingPlayback: Boolean = false,
    val detail: DramaDetail? = null,
    val currentSeasonIndex: Int = 1,
    val currentEpisode: Episode? = null,
    val playbackSource: PlaybackSource? = null,
    val isBookmarked: Boolean = false,
    val isCurrentEpisodeDownloaded: Boolean = false,
    val showLicenseGate: Boolean = false,
    val lockedEpisodeNumber: Int = 1,
    val errorMessage: String? = null
)

class VodPlayerViewModel(
    val providerId: String,
    val dramaId: String,
    private val catalogRepository: CatalogRepository,
    private val watchHistoryDao: WatchHistoryDao,
    private val bookmarkDao: BookmarkDao,
    private val entitlementManager: EntitlementManager,
    private val licenseRepository: LicenseRepository,
    playerFactory: PlayerFactory,
    enablePlayerCache: Boolean = true,
    private val downloadRecordDao: DownloadRecordDao? = null,
    private val downloadTracker: DownloadTracker? = null
) : ViewModel() {

    private val playerPair = playerFactory.createPlayer(enableCache = enablePlayerCache)
    val playerController: DramixPlayerController = DramixPlayerController(
        player = playerPair.first,
        headerDataSourceFactory = playerPair.second,
        scope = viewModelScope
    )

    private val _uiState = MutableStateFlow(VodPlayerUiState())
    val uiState: StateFlow<VodPlayerUiState> = _uiState.asStateFlow()

    private var historyTrackingJob: Job? = null

    init {
        observeBookmarkStatus()
        loadDramaDetailAndInitialEpisode()
        startHistoryAutosave()
    }

    private fun observeBookmarkStatus() {
        viewModelScope.launch {
            bookmarkDao.isBookmarked(dramaId, providerId).collect { bookmarked ->
                _uiState.value = _uiState.value.copy(isBookmarked = bookmarked)
            }
        }
    }

    fun loadDramaDetailAndInitialEpisode() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDetail = true, errorMessage = null)

            val detailResult = catalogRepository.getDramaDetail(providerId, dramaId)
            if (detailResult.isFailure) {
                // Airplane Mode / Network Failure: Check if offline downloads exist for this drama
                val offlineRecords = try {
                    downloadRecordDao?.getDownloadsByStatus("COMPLETED")?.firstOrNull()
                        ?.filter { it.dramaId == dramaId && it.providerId == providerId }
                        ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }

                if (offlineRecords.isNotEmpty()) {
                    val offlineEpisodes = offlineRecords.map { rec ->
                        Episode(
                            id = rec.mediaId,
                            number = rec.episodeNumber,
                            title = rec.episodeTitle,
                            isVip = false
                        )
                    }.sortedBy { it.number }

                    val offlineDetail = DramaDetail(
                        id = dramaId,
                        title = offlineRecords.first().dramaTitle,
                        description = "Tersedia offline di perangkat Anda",
                        cover = "",
                        totalEpisodes = offlineEpisodes.size,
                        seasons = listOf(Season(index = 1, totalEpisodes = offlineEpisodes.size, episodes = offlineEpisodes))
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoadingDetail = false,
                        detail = offlineDetail
                    )
                    playEpisode(offlineEpisodes.first())
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoadingDetail = false,
                    errorMessage = detailResult.exceptionOrNull()?.localizedMessage ?: "Gagal memuat detail drama"
                )
                return@launch
            }

            val detail = detailResult.getOrThrow()
            _uiState.value = _uiState.value.copy(
                isLoadingDetail = false,
                detail = detail
            )

            // Determine starting episode: check if user watched this drama before
            val allEpisodes = detail.seasons.firstOrNull()?.episodes ?: emptyList()
            if (allEpisodes.isEmpty()) return@launch

            // Find latest watched episode from DB
            val latestHistory = try {
                watchHistoryDao.getEpisodeHistory(dramaId, providerId, 1)
            } catch (_: Exception) {
                null
            }

            val targetEpisode = if (latestHistory != null) {
                allEpisodes.find { it.number == latestHistory.episodeNumber } ?: allEpisodes.first()
            } else {
                allEpisodes.first()
            }

            playEpisode(targetEpisode)
        }
    }

    fun playEpisode(episode: Episode) {
        val access = entitlementManager.canPlayEpisode(episode.number, episode.isVip)
        if (access is PlaybackAccess.AccessDenied) {
            playerController.pause()
            _uiState.value = _uiState.value.copy(
                showLicenseGate = true,
                lockedEpisodeNumber = episode.number,
                isLoadingPlayback = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            currentEpisode = episode,
            showLicenseGate = false,
            isLoadingPlayback = true,
            errorMessage = null
        )

        viewModelScope.launch {
            // Save last position of old episode if switching
            saveCurrentProgress()

            // Check if episode is already downloaded locally
            val localRecord = try {
                downloadRecordDao?.getDownloadByMediaIdSync(episode.id)
            } catch (_: Exception) {
                null
            }

            if (localRecord != null && localRecord.status == "COMPLETED" && !localRecord.localUri.isNullOrBlank()) {
                val resumePosition = try {
                    val epHistory = watchHistoryDao.getEpisodeHistory(dramaId, providerId, episode.number)
                    epHistory?.positionMs ?: 0L
                } catch (_: Exception) {
                    0L
                }

                playerController.prepare(
                    streamUrl = localRecord.localUri,
                    headers = emptyMap(),
                    startPositionMs = resumePosition,
                    autoPlay = true
                )

                _uiState.value = _uiState.value.copy(
                    isLoadingPlayback = false,
                    isCurrentEpisodeDownloaded = true
                )
                return@launch
            }

            val sourceResult = catalogRepository.getPlaybackSource(
                modelId = providerId,
                episodeId = episode.id,
                id = dramaId
            )

            if (sourceResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoadingPlayback = false,
                    errorMessage = sourceResult.exceptionOrNull()?.localizedMessage ?: "Gagal memuat sumber video"
                )
                return@launch
            }

            val source = sourceResult.getOrThrow()
            val stream = source.streams.firstOrNull()

            if (stream == null) {
                _uiState.value = _uiState.value.copy(
                    isLoadingPlayback = false,
                    errorMessage = "Tidak ada stream video yang tersedia"
                )
                return@launch
            }

            // Retrieve resume position for this specific episode
            val resumePosition = try {
                val epHistory = watchHistoryDao.getEpisodeHistory(dramaId, providerId, episode.number)
                epHistory?.positionMs ?: 0L
            } catch (_: Exception) {
                0L
            }

            // Combine stream-level and source-level headers
            val headers = HashMap<String, String>().apply {
                putAll(source.headers)
                putAll(stream.headers)
            }

            playerController.prepare(
                streamUrl = stream.url,
                headers = headers,
                startPositionMs = resumePosition,
                autoPlay = true
            )

            _uiState.value = _uiState.value.copy(
                playbackSource = source,
                isLoadingPlayback = false,
                isCurrentEpisodeDownloaded = localRecord?.status == "COMPLETED"
            )
        }
    }

    fun downloadCurrentEpisode() {
        val ep = _uiState.value.currentEpisode ?: return
        val detail = _uiState.value.detail ?: return
        val source = _uiState.value.playbackSource ?: return
        val stream = source.streams.firstOrNull() ?: return

        val headers = HashMap<String, String>().apply {
            putAll(source.headers)
            putAll(stream.headers)
        }

        downloadTracker?.startDownload(
            dramaId = dramaId,
            providerId = providerId,
            dramaTitle = detail.title,
            episodeNumber = ep.number,
            episodeTitle = ep.title,
            streamUrl = stream.url,
            mediaId = ep.id,
            headers = headers
        )
    }

    fun toggleBookmark() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            val currentlyBookmarked = _uiState.value.isBookmarked
            if (currentlyBookmarked) {
                bookmarkDao.deleteBookmark(dramaId, providerId)
            } else {
                val bookmark = BookmarkEntity(
                    dramaId = dramaId,
                    providerId = providerId,
                    title = detail.title,
                    posterUrl = detail.cover,
                    contentType = detail.type ?: "long_drama",
                    rating = detail.score,
                    totalEpisodes = detail.totalEpisodes
                )
                bookmarkDao.insertBookmark(bookmark)
            }
        }
    }

    fun dismissLicenseGate() {
        _uiState.value = _uiState.value.copy(showLicenseGate = false)
    }

    fun selectSeason(seasonIndex: Int) {
        _uiState.value = _uiState.value.copy(currentSeasonIndex = seasonIndex)
    }

    suspend fun saveCurrentProgress() {
        val currentEp = _uiState.value.currentEpisode ?: return
        val detail = _uiState.value.detail ?: return
        val player = playerController.player

        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val durationMs = player.duration.coerceAtLeast(0L)

        if (durationMs > 0L && positionMs > 0L) {
            val isCompleted = positionMs >= (durationMs * 0.95)
            val history = WatchHistoryEntity(
                dramaId = dramaId,
                providerId = providerId,
                dramaTitle = detail.title,
                dramaPoster = detail.cover,
                episodeNumber = currentEp.number,
                episodeTitle = currentEp.title,
                positionMs = positionMs,
                durationMs = durationMs,
                isCompleted = isCompleted,
                updatedAt = System.currentTimeMillis()
            )
            watchHistoryDao.insertOrUpdateWatchHistory(history)
        }
    }

    private fun startHistoryAutosave() {
        historyTrackingJob?.cancel()
        historyTrackingJob = viewModelScope.launch {
            while (isActive) {
                delay(5000L) // Autosave every 5 seconds
                saveCurrentProgress()
            }
        }
    }

    fun release() {
        historyTrackingJob?.cancel()
        historyTrackingJob = null
        playerController.release()
    }

    override fun onCleared() {
        release()
        super.onCleared()
    }
}
