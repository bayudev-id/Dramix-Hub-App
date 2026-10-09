package com.dramix.app.ui.screens.player_vod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dramix.app.core.database.dao.BookmarkDao
import com.dramix.app.core.database.dao.DownloadRecordDao
import com.dramix.app.core.database.dao.WatchHistoryDao
import com.dramix.app.core.database.entity.BookmarkEntity
import com.dramix.app.core.database.entity.WatchHistoryEntity
import com.dramix.app.data.source.local.PlayerPreferences
import com.dramix.app.domain.manager.EntitlementManager
import com.dramix.app.domain.manager.PlaybackAccess
import com.dramix.app.domain.model.DramaDetail
import com.dramix.app.domain.model.Dub
import com.dramix.app.domain.model.Episode
import com.dramix.app.domain.model.PlaybackSource
import com.dramix.app.domain.model.Season
import com.dramix.app.domain.model.StreamSource
import com.dramix.app.domain.repository.CatalogRepository
import com.dramix.app.domain.repository.LicenseRepository
import com.dramix.app.player.controller.DramixPlayerController
import com.dramix.app.player.download.DownloadTracker
import com.dramix.app.player.engine.PlayerFactory
import com.dramix.app.player.subtitle.SubtitleManager
import com.dramix.app.player.subtitle.SubtitleParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SubtitleStyleConfig(
    val fontSizePx: Int = 28,
    val positionPercent: Int = 10,
    val backgroundOpacityPercent: Int = 50,
    val backgroundPaddingPx: Int = 16,
    val lineSpacingPx: Int = 4,
    val textColor: Long = 0xFFFFFFFF,
    val baseBackgroundColor: Long = 0xFF000000,
    val fontFamily: String = "Arial",
    val outlineStyle: String = "Thin"
) {
    val backgroundColor: Long
        get() {
            val alpha = ((backgroundOpacityPercent.coerceIn(0, 100) * 255 + 50) / 100).toLong() and 0xFF
            return (alpha shl 24) or (baseBackgroundColor and 0x00FFFFFF)
        }
}

data class SubtitleUiModel(
    val id: String,
    val label: String,
    val language: String? = null,
    val url: String? = null
)

data class VodPlayerUiState(
    val isLoadingDetail: Boolean = true,
    val isLoadingPlayback: Boolean = false,
    val isRefreshing: Boolean = false,
    val detail: DramaDetail? = null,
    val currentSeasonIndex: Int = 1,
    val currentEpisode: Episode? = null,
    val playbackSource: PlaybackSource? = null,
    val isBookmarked: Boolean = false,
    val isCurrentEpisodeDownloaded: Boolean = false,
    val showLicenseGate: Boolean = false,
    val lockedEpisodeNumber: Int = 1,
    val rentalBlockedEpisode: Episode? = null,
    val errorMessage: String? = null,
    val initialPositionMs: Long = 0L,
    val prefilledDurationMs: Long = 0L,
    val availableQualities: List<String> = emptyList(),
    val selectedQuality: String? = null,
    val selectedStream: StreamSource? = null,
    val availableSubtitles: List<SubtitleUiModel> = emptyList(),
    val selectedSubtitleId: String = "off",
    val selectedDubId: String? = null,
    val playbackSpeed: Float = 1.0f,
    val isAutoNext: Boolean = false,
    val videoZoom: String = "100%",
    val portraitSubtitleStyle: SubtitleStyleConfig = SubtitleStyleConfig(fontSizePx = 20),
    val fullscreenSubtitleStyle: SubtitleStyleConfig = SubtitleStyleConfig(fontSizePx = 28),
    val subtitleStyle: SubtitleStyleConfig = SubtitleStyleConfig()
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
    enablePlayerCache: Boolean = false,
    private val downloadRecordDao: DownloadRecordDao? = null,
    private val downloadTracker: DownloadTracker? = null,
    private val playerPreferences: PlayerPreferences? = null
) : ViewModel() {

    private val playerPair = playerFactory.createPlayer(enableCache = enablePlayerCache)
    val playerController: DramixPlayerController = DramixPlayerController(
        player = playerPair.first,
        headerDataSourceFactory = playerPair.second,
        scope = viewModelScope
    )
    
    private val subtitleManager = SubtitleManager()

    private var activeDramaId: String = dramaId
    private var bookmarkJob: Job? = null
    private var subtitleSyncJob: Job? = null

    private val _uiState = MutableStateFlow(
        run {
            val portraitStyle = playerPreferences?.getSubtitleStyle(isFullscreen = false)
                ?: SubtitleStyleConfig(fontSizePx = 20)
            val fullscreenStyle = playerPreferences?.getSubtitleStyle(isFullscreen = true)
                ?: SubtitleStyleConfig(fontSizePx = 28)
            VodPlayerUiState(
                videoZoom = playerPreferences?.getVideoZoom() ?: "100%",
                playbackSpeed = playerPreferences?.getPlaybackSpeed() ?: 1.0f,
                isAutoNext = playerPreferences?.isAutoNext() ?: false,
                portraitSubtitleStyle = portraitStyle,
                fullscreenSubtitleStyle = fullscreenStyle,
                subtitleStyle = fullscreenStyle
            )
        }
    )
    val uiState: StateFlow<VodPlayerUiState> = _uiState.asStateFlow()

    private var historyTrackingJob: Job? = null

    val currentSubtitleText = subtitleManager.currentSubtitle

    init {
        observeBookmarkStatus()
        loadDramaDetailAndInitialEpisode()
        startHistoryAutosave()
        observePlaybackEndForAutoNext()
    }

    private fun observeBookmarkStatus() {
        bookmarkJob?.cancel()
        bookmarkJob = viewModelScope.launch {
            bookmarkDao.isBookmarked(activeDramaId, providerId).collect { bookmarked ->
                _uiState.value = _uiState.value.copy(isBookmarked = bookmarked)
            }
        }
    }

    fun loadDramaDetailAndInitialEpisode(
        overrideResumePosition: Long? = null,
        preferredDubId: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDetail = true, errorMessage = null)

            val detailResult = catalogRepository.getDramaDetail(providerId, activeDramaId)
            if (detailResult.isFailure) {
                // Airplane Mode / Network Failure: Check if offline downloads exist for this drama
                val offlineRecords = try {
                    downloadRecordDao?.getDownloadsByStatus("COMPLETED")?.firstOrNull()
                        ?.filter { it.dramaId == activeDramaId && it.providerId == providerId }
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
                        id = activeDramaId,
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
                    playEpisode(offlineEpisodes.first(), overridePositionMs = overrideResumePosition)
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoadingDetail = false,
                    errorMessage = detailResult.exceptionOrNull()?.localizedMessage ?: "Gagal memuat detail drama"
                )
                return@launch
            }

            val detail = detailResult.getOrThrow()

            // Resolve active dub
            val resolvedDubId = preferredDubId
                ?: detail.dubs.find { it.id == activeDramaId || activeDramaId.contains(it.id) || it.id.contains(activeDramaId) }?.id
                ?: detail.dubs.firstOrNull { it.isOriginal }?.id
                ?: detail.dubs.firstOrNull()?.id

            _uiState.value = _uiState.value.copy(
                isLoadingDetail = false,
                detail = detail,
                selectedDubId = resolvedDubId
            )

            // Determine starting episode: check if user watched this drama before
            val allEpisodes = detail.seasons.firstOrNull()?.episodes ?: emptyList()
            if (allEpisodes.isEmpty()) return@launch

            // Find latest watched episode from DB
            val latestHistory = try {
                watchHistoryDao.getLatestWatchedEpisode(activeDramaId, providerId)
            } catch (_: Exception) {
                null
            }

            val targetEpisode = if (latestHistory != null) {
                allEpisodes.find { it.number == latestHistory.episodeNumber } ?: allEpisodes.first()
            } else {
                allEpisodes.first()
            }

            playEpisode(targetEpisode, overridePositionMs = overrideResumePosition)
        }
    }

    fun selectDub(dub: Dub) {
        if (dub.id.isBlank() || dub.id == activeDramaId) return

        val player = playerController.player
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val durationMs = player.duration.coerceAtLeast(0L)
        val detail = _uiState.value.detail
        if (detail != null && durationMs > 0L && positionMs > 0L) {
            val isCompleted = positionMs >= (durationMs * 0.95)
            val history = WatchHistoryEntity(
                dramaId = activeDramaId,
                providerId = providerId,
                dramaTitle = detail.title,
                dramaPoster = detail.cover,
                episodeNumber = _uiState.value.currentEpisode?.number ?: 1,
                episodeTitle = _uiState.value.currentEpisode?.title ?: "Episode 1",
                positionMs = positionMs,
                durationMs = durationMs,
                isCompleted = isCompleted,
                updatedAt = System.currentTimeMillis()
            )
            viewModelScope.launch {
                watchHistoryDao.insertOrUpdateWatchHistory(history)
            }
        }

        playerController.pause()
        activeDramaId = dub.id
        observeBookmarkStatus()
        loadDramaDetailAndInitialEpisode(
            overrideResumePosition = if (positionMs > 0L) positionMs else null,
            preferredDubId = dub.id
        )
    }

    fun playEpisode(episode: Episode, overridePositionMs: Long? = null) {
        android.util.Log.d("VodPlayer", "playEpisode called: ${episode.id} ${episode.title}")
        if (!episode.isSewa) {
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
        }

        // 1. Save progress of the previous episode BEFORE updating currentEpisode
        val previousEpisode = _uiState.value.currentEpisode
        if (previousEpisode != null && previousEpisode.id != episode.id) {
            val player = playerController.player
            val positionMs = player.currentPosition.coerceAtLeast(0L)
            val durationMs = player.duration.coerceAtLeast(0L)
            val detail = _uiState.value.detail
            if (detail != null && durationMs > 0L && positionMs > 0L) {
                val isCompleted = positionMs >= (durationMs * 0.95)
                val history = WatchHistoryEntity(
                    dramaId = activeDramaId,
                    providerId = providerId,
                    dramaTitle = detail.title,
                    dramaPoster = detail.cover,
                    episodeNumber = previousEpisode.number,
                    episodeTitle = previousEpisode.title,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    isCompleted = isCompleted,
                    updatedAt = System.currentTimeMillis()
                )
                viewModelScope.launch {
                    watchHistoryDao.insertOrUpdateWatchHistory(history)
                }
            }
        }

        val initialEstimateDuration = if ((episode.durationSeconds ?: 0) > 0) {
            episode.durationSeconds!! * 1000L
        } else {
            0L
        }

        val targetSeason = _uiState.value.detail?.seasons?.find { s -> s.episodes.any { it.id == episode.id } }
        val newSeasonIndex = targetSeason?.index ?: _uiState.value.currentSeasonIndex

        _uiState.value = _uiState.value.copy(
            currentEpisode = episode,
            currentSeasonIndex = newSeasonIndex,
            showLicenseGate = false,
            isLoadingPlayback = true,
            errorMessage = null,
            initialPositionMs = 0L,
            prefilledDurationMs = initialEstimateDuration
        )

        viewModelScope.launch {
            // Retrieve resume position and saved duration immediately from history
            val epHistory = try {
                watchHistoryDao.getEpisodeHistory(activeDramaId, providerId, episode.number)
            } catch (_: Exception) {
                null
            }

            val resumePosition = overridePositionMs
                ?: if (epHistory != null && !epHistory.isCompleted) epHistory.positionMs else 0L
            val savedDuration = epHistory?.durationMs?.takeIf { it > 0L } ?: initialEstimateDuration

            _uiState.value = _uiState.value.copy(
                initialPositionMs = resumePosition,
                prefilledDurationMs = savedDuration
            )

            // Check if episode is already downloaded locally
            val localRecord = try {
                downloadRecordDao?.getDownloadByMediaIdSync(episode.id)
            } catch (_: Exception) {
                null
            }

            if (localRecord != null && localRecord.status == "COMPLETED" && !localRecord.localUri.isNullOrBlank()) {
                playerController.prepare(
                    streamUrl = localRecord.localUri,
                    headers = emptyMap(),
                    startPositionMs = resumePosition,
                    autoPlay = true
                )

                _uiState.value = _uiState.value.copy(
                    isLoadingPlayback = false,
                    isCurrentEpisodeDownloaded = true,
                    availableQualities = listOf("Offline"),
                    selectedQuality = "Offline",
                    availableSubtitles = emptyList(),
                    selectedSubtitleId = "off"
                )
                return@launch
            }

            val sourceResult = catalogRepository.getPlaybackSource(
                modelId = providerId,
                episodeId = episode.id,
                id = activeDramaId
            )

            if (sourceResult.isFailure) {
                val error = sourceResult.exceptionOrNull()
                android.util.Log.e("VodPlayer", "Fetch source failed: ${error?.message}")
                if (episode.isSewa) {
                    playerController.pause()
                    _uiState.value = _uiState.value.copy(
                        isLoadingPlayback = false,
                        rentalBlockedEpisode = episode,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingPlayback = false,
                        errorMessage = error?.localizedMessage ?: "Gagal memuat sumber video"
                    )
                }
                return@launch
            }

            val source = sourceResult.getOrThrow()
            android.util.Log.d("VodPlayer", "Source loaded: streams=${source.streams.size}, subs=${source.subtitles.size}")
            val stream = source.streams.firstOrNull()

            if (stream == null) {
                android.util.Log.e("VodPlayer", "No stream available")
                if (episode.isSewa) {
                    playerController.pause()
                    _uiState.value = _uiState.value.copy(
                        isLoadingPlayback = false,
                        rentalBlockedEpisode = episode,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingPlayback = false,
                        errorMessage = "Tidak ada stream video yang tersedia"
                    )
                }
                return@launch
            }

            val resolvedDuration = when {
                savedDuration > 0L -> savedDuration
                (source.durationSeconds ?: 0) > 0 -> source.durationSeconds!! * 1000L
                else -> 0L
            }

            // Extract qualities and sort highest quality first
            val rawQualities = if (source.streams.size > 1) {
                source.streams.mapNotNull { it.quality?.let { q -> formatQualityLabel(q) } }.distinct()
            } else {
                val q = stream.quality
                if (!q.isNullOrBlank() && !q.equals("auto", ignoreCase = true)) {
                    listOf(formatQualityLabel(q))
                } else {
                    listOf("Auto")
                }
            }
            val qualities = sortQualitiesDescending(rawQualities)
            val savedQuality = playerPreferences?.getPreferredQuality()
            val defaultQuality = if (!savedQuality.isNullOrBlank() && qualities.contains(savedQuality)) {
                savedQuality
            } else {
                qualities.firstOrNull() ?: "Auto"
            }
            val defaultStream = source.streams.find {
                it.quality?.equals(defaultQuality, ignoreCase = true) == true ||
                    formatQualityLabel(it.quality ?: "").equals(defaultQuality, ignoreCase = true)
            } ?: stream

            // Extract subtitles
            val subtitleOptions = mutableListOf<SubtitleUiModel>()
            subtitleOptions.add(SubtitleUiModel(id = "off", label = "Off"))

            source.subtitles.forEach { sub ->
                val id = sub.lang?.lowercase()?.trim() ?: sub.url
                val label = formatSubtitleLabel(sub.lang, sub.label, sub.url)
                subtitleOptions.add(
                    SubtitleUiModel(
                        id = id,
                        label = label,
                        language = sub.lang?.lowercase()?.trim(),
                        url = sub.url
                    )
                )
            }

            val savedSubId = playerPreferences?.getPreferredSubtitleId()
            val matchedSavedSub = if (!savedSubId.isNullOrBlank() && savedSubId != "off") {
                subtitleOptions.find { it.id.equals(savedSubId, ignoreCase = true) || it.language?.equals(savedSubId, ignoreCase = true) == true }
            } else null

            val defaultSub = matchedSavedSub ?: subtitleOptions.find {
                it.id.equals("id", ignoreCase = true) ||
                    it.id.equals("in", ignoreCase = true) ||
                    it.id.equals("in_id", ignoreCase = true) ||
                    it.id.contains("indo", ignoreCase = true) ||
                    it.label.contains("Indo", ignoreCase = true) ||
                    it.language?.contains("id") == true ||
                    it.language?.contains("in") == true
            }
            val defaultSubId = if (savedSubId == "off") "off" else (defaultSub?.id ?: "off")

            // Combine stream-level and source-level headers
            val headers = HashMap<String, String>().apply {
                putAll(source.headers)
                putAll(defaultStream.headers)
                if (providerId.equals("moviebox", ignoreCase = true) || defaultStream.url.contains("hakunaymatata.com")) {
                    if (!containsKey("Referer")) {
                        put("Referer", "https://moviebox.ph/")
                    }
                }
            }

            android.util.Log.d("VodPlayer", "Preparing player with URL: ${defaultStream.url.take(100)}... format=${defaultStream.format}")
            playerController.prepare(
                streamUrl = defaultStream.url,
                headers = headers,
                startPositionMs = resumePosition,
                autoPlay = true,
                subtitles = source.subtitles,
                preferredSubtitleLang = if (defaultSubId != "off") defaultSubId else null
            )
            android.util.Log.d("VodPlayer", "Player prepared successfully")
            playerController.setPlaybackSpeed(_uiState.value.playbackSpeed)

            _uiState.value = _uiState.value.copy(
                playbackSource = source,
                isLoadingPlayback = false,
                isCurrentEpisodeDownloaded = localRecord?.status == "COMPLETED",
                prefilledDurationMs = resolvedDuration,
                availableQualities = qualities,
                selectedQuality = defaultQuality,
                selectedStream = defaultStream,
                availableSubtitles = subtitleOptions,
                selectedSubtitleId = defaultSubId
            )
            
            fetchAndLoadCustomSubtitles(source.subtitles, defaultSubId)
        }
    }

    fun selectQuality(quality: String) {
        val source = _uiState.value.playbackSource ?: return
        val targetStream = source.streams.find {
            it.quality?.equals(quality, ignoreCase = true) == true ||
                formatQualityLabel(it.quality ?: "").equals(quality, ignoreCase = true)
        } ?: source.streams.firstOrNull() ?: return

        if (targetStream == _uiState.value.selectedStream && quality == _uiState.value.selectedQuality) {
            return
        }

        val player = playerController.player
        val currentPosition = player.currentPosition.coerceAtLeast(0L)
        val isPlaying = player.isPlaying

        val headers = HashMap<String, String>().apply {
            putAll(source.headers)
            putAll(targetStream.headers)
            if (providerId.equals("moviebox", ignoreCase = true) || targetStream.url.contains("hakunaymatata.com")) {
                if (!containsKey("Referer")) {
                    put("Referer", "https://moviebox.ph/")
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            selectedStream = targetStream,
            selectedQuality = quality
        )
        playerPreferences?.savePreferredQuality(quality)

        playerController.prepare(
            streamUrl = targetStream.url,
            headers = headers,
            startPositionMs = currentPosition,
            autoPlay = isPlaying,
            subtitles = source.subtitles,
            preferredSubtitleLang = _uiState.value.selectedSubtitleId.takeIf { it != "off" }
        )
        playerController.setPlaybackSpeed(_uiState.value.playbackSpeed)
        
        fetchAndLoadCustomSubtitles(source.subtitles, _uiState.value.selectedSubtitleId)
    }

    fun selectSubtitle(subtitleId: String) {
        _uiState.value = _uiState.value.copy(selectedSubtitleId = subtitleId)
        playerPreferences?.savePreferredSubtitleId(subtitleId)
        if (subtitleId.equals("off", ignoreCase = true)) {
            playerController.setSubtitleLanguage(null)
            subtitleManager.clear()
        } else {
            val targetSub = _uiState.value.availableSubtitles.find { it.id.equals(subtitleId, ignoreCase = true) }
            playerController.setSubtitleLanguage(targetSub?.language ?: subtitleId)
            
            val source = _uiState.value.playbackSource
            if (source != null) {
                fetchAndLoadCustomSubtitles(source.subtitles, subtitleId)
            }
        }
    }
    
    private fun fetchAndLoadCustomSubtitles(subtitles: List<com.dramix.app.domain.model.Subtitle>, selectedId: String) {
        if (selectedId.equals("off", ignoreCase = true)) {
            subtitleManager.clear()
            stopSubtitleSync()
            return
        }
        
        viewModelScope.launch {
            try {
                val targetSubtitle = subtitles.find { sub ->
                    val id = sub.lang?.lowercase()?.trim() ?: sub.url
                    id.equals(selectedId, ignoreCase = true) || sub.lang?.lowercase()?.equals(selectedId, ignoreCase = true) == true
                }
                
                if (targetSubtitle == null || targetSubtitle.url.isBlank()) {
                    subtitleManager.clear()
                    stopSubtitleSync()
                    return@launch
                }
                
                val content = fetchSubtitleContent(targetSubtitle.url)
                val cues = SubtitleParser.parseAuto(content)
                subtitleManager.setSubtitles(cues)
                startSubtitleSync()
                
                android.util.Log.d("VodPlayer", "Custom subtitle loaded: ${cues.size} cues")
            } catch (e: Exception) {
                android.util.Log.e("VodPlayer", "Failed to load custom subtitle: ${e.message}")
                subtitleManager.clear()
                stopSubtitleSync()
            }
        }
    }
    
    private suspend fun fetchSubtitleContent(url: String): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            try {
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
    }
    
    private fun startSubtitleSync() {
        stopSubtitleSync()
        subtitleSyncJob = viewModelScope.launch {
            while (isActive) {
                val positionMs = playerController.player.currentPosition.coerceAtLeast(0L)
                subtitleManager.updatePosition(positionMs)
                delay(100L)
            }
        }
    }
    
    private fun stopSubtitleSync() {
        subtitleSyncJob?.cancel()
        subtitleSyncJob = null
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
        playerPreferences?.savePlaybackSpeed(speed)
        playerController.setPlaybackSpeed(speed)
    }

    fun toggleAutoNext(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAutoNext = enabled)
        playerPreferences?.saveAutoNext(enabled)
    }

    fun selectVideoZoom(zoom: String) {
        _uiState.value = _uiState.value.copy(videoZoom = zoom)
        playerPreferences?.saveVideoZoom(zoom)
    }

    fun updateVideoZoom(delta: Int) {
        val current = _uiState.value.videoZoom
        val currentPct = current.removeSuffix("%").trim().toIntOrNull() ?: 100
        val newPct = (currentPct + delta).coerceIn(100, 300)
        val zoomStr = "${newPct}%"
        _uiState.value = _uiState.value.copy(videoZoom = zoomStr)
        playerPreferences?.saveVideoZoom(zoomStr)
    }

    fun hasPreviousEpisode(): Boolean {
        val detail = _uiState.value.detail ?: return false
        val currentEp = _uiState.value.currentEpisode ?: return false
        val allEpisodes = detail.seasons.flatMap { it.episodes }
        val currentIndex = allEpisodes.indexOfFirst { it.id == currentEp.id }
        return currentIndex > 0
    }

    fun hasNextEpisode(): Boolean {
        val detail = _uiState.value.detail ?: return false
        val currentEp = _uiState.value.currentEpisode ?: return false
        val allEpisodes = detail.seasons.flatMap { it.episodes }
        val currentIndex = allEpisodes.indexOfFirst { it.id == currentEp.id }
        return currentIndex in 0 until (allEpisodes.size - 1)
    }

    fun playPreviousEpisode() {
        val detail = _uiState.value.detail ?: return
        val currentEp = _uiState.value.currentEpisode ?: return
        val allEpisodes = detail.seasons.flatMap { it.episodes }
        val currentIndex = allEpisodes.indexOfFirst { it.id == currentEp.id }
        if (currentIndex > 0) {
            playEpisode(allEpisodes[currentIndex - 1])
        }
    }

    fun playNextEpisode() {
        val detail = _uiState.value.detail ?: return
        val currentEp = _uiState.value.currentEpisode ?: return
        val allEpisodes = detail.seasons.flatMap { it.episodes }
        val currentIndex = allEpisodes.indexOfFirst { it.id == currentEp.id }
        if (currentIndex in 0 until (allEpisodes.size - 1)) {
            playEpisode(allEpisodes[currentIndex + 1])
        }
    }

    private fun observePlaybackEndForAutoNext() {
        viewModelScope.launch {
            playerController.playbackState.collect { state ->
                if (state is com.dramix.app.player.model.PlaybackState.Ended && _uiState.value.isAutoNext) {
                    if (hasNextEpisode()) {
                        playNextEpisode()
                    }
                }
            }
        }
    }

    private fun persistSubtitleStyle(style: SubtitleStyleConfig, isFullscreen: Boolean) {
        if (isFullscreen) {
            _uiState.value = _uiState.value.copy(
                fullscreenSubtitleStyle = style,
                subtitleStyle = style
            )
        } else {
            _uiState.value = _uiState.value.copy(
                portraitSubtitleStyle = style,
                subtitleStyle = style
            )
        }
        playerPreferences?.saveSubtitleStyle(style, isFullscreen = isFullscreen)
    }

    private fun getActiveSubtitleStyle(isFullscreen: Boolean): SubtitleStyleConfig {
        return if (isFullscreen) _uiState.value.fullscreenSubtitleStyle else _uiState.value.portraitSubtitleStyle
    }

    fun updateSubtitleFontSize(delta: Int, isFullscreen: Boolean = true) {
        val current = getActiveSubtitleStyle(isFullscreen)
        val minSize = if (isFullscreen) 16 else 12
        val maxSize = if (isFullscreen) 80 else 48
        val newSize = (current.fontSizePx + delta).coerceIn(minSize, maxSize)
        persistSubtitleStyle(current.copy(fontSizePx = newSize), isFullscreen)
    }

    fun updateSubtitlePosition(delta: Int, isFullscreen: Boolean = true) {
        val current = getActiveSubtitleStyle(isFullscreen)
        val newPos = (current.positionPercent + delta).coerceIn(0, 50)
        persistSubtitleStyle(current.copy(positionPercent = newPos), isFullscreen)
    }

    fun updateSubtitleBgOpacity(delta: Int, isFullscreen: Boolean = true) {
        val current = getActiveSubtitleStyle(isFullscreen)
        val newOpacity = (current.backgroundOpacityPercent + delta).coerceIn(0, 100)
        persistSubtitleStyle(current.copy(backgroundOpacityPercent = newOpacity), isFullscreen)
    }

    fun setSubtitleBgOpacity(opacity: Int, isFullscreen: Boolean = true) {
        val current = getActiveSubtitleStyle(isFullscreen)
        persistSubtitleStyle(current.copy(backgroundOpacityPercent = opacity.coerceIn(0, 100)), isFullscreen)
    }

    fun updateSubtitleTextColor(color: Long, isFullscreen: Boolean = true) {
        val current = getActiveSubtitleStyle(isFullscreen)
        persistSubtitleStyle(current.copy(textColor = color), isFullscreen)
    }

    fun updateSubtitleBgColor(color: Long, isFullscreen: Boolean = true) {
        val current = getActiveSubtitleStyle(isFullscreen)
        val alpha = ((color ushr 24) and 0xFF).toInt()
        val opacity = ((alpha * 100) + 127) / 255
        persistSubtitleStyle(
            current.copy(
                baseBackgroundColor = color or 0xFF000000L,
                backgroundOpacityPercent = opacity.coerceIn(0, 100)
            ),
            isFullscreen
        )
    }

    fun selectSubtitleFontFamily(font: String, isFullscreen: Boolean = true) {
        val current = getActiveSubtitleStyle(isFullscreen)
        persistSubtitleStyle(current.copy(fontFamily = font), isFullscreen)
    }

    fun selectSubtitleOutlineStyle(outline: String, isFullscreen: Boolean = true) {
        val current = getActiveSubtitleStyle(isFullscreen)
        persistSubtitleStyle(current.copy(outlineStyle = outline), isFullscreen)
    }

    fun updateSubtitleLineSpacing(delta: Int, isFullscreen: Boolean = true) {
        val current = getActiveSubtitleStyle(isFullscreen)
        val newSpacing = (current.lineSpacingPx + delta).coerceIn(0, 20)
        persistSubtitleStyle(current.copy(lineSpacingPx = newSpacing), isFullscreen)
    }

    fun setSubtitleLineSpacing(spacing: Int, isFullscreen: Boolean = true) {
        val current = getActiveSubtitleStyle(isFullscreen)
        persistSubtitleStyle(current.copy(lineSpacingPx = spacing.coerceIn(0, 20)), isFullscreen)
    }

    fun updateSubtitleBackgroundPadding(delta: Int, isFullscreen: Boolean = true) {
        val current = getActiveSubtitleStyle(isFullscreen)
        val newPadding = (current.backgroundPaddingPx + delta).coerceIn(8, 48)
        persistSubtitleStyle(current.copy(backgroundPaddingPx = newPadding), isFullscreen)
    }

    fun setSubtitleBackgroundPadding(padding: Int, isFullscreen: Boolean = true) {
        val current = getActiveSubtitleStyle(isFullscreen)
        persistSubtitleStyle(current.copy(backgroundPaddingPx = padding.coerceIn(8, 48)), isFullscreen)
    }

    fun downloadCurrentEpisode() {
        val ep = _uiState.value.currentEpisode ?: return
        val detail = _uiState.value.detail ?: return
        val source = _uiState.value.playbackSource ?: return
        val stream = source.streams.firstOrNull() ?: return

        val headers = HashMap<String, String>().apply {
            putAll(source.headers)
            putAll(stream.headers)
            if (providerId.equals("moviebox", ignoreCase = true) || stream.url.contains("hakunaymatata.com")) {
                if (!containsKey("Referer")) {
                    put("Referer", "https://moviebox.ph/")
                }
            }
        }

        downloadTracker?.startDownload(
            dramaId = activeDramaId,
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
                bookmarkDao.deleteBookmark(activeDramaId, providerId)
            } else {
                val bookmark = BookmarkEntity(
                    dramaId = activeDramaId,
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

    fun dismissRentalGate() {
        _uiState.value = _uiState.value.copy(rentalBlockedEpisode = null)
    }

    fun refreshCurrentEpisode() {
        val episode = _uiState.value.currentEpisode ?: return
        if (_uiState.value.isRefreshing) return
        
        _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val sourceResult = catalogRepository.getPlaybackSource(
                    modelId = providerId,
                    episodeId = episode.id,
                    id = activeDramaId
                )
                
                if (sourceResult.isSuccess) {
                    val source = sourceResult.getOrThrow()
                    val stream = source.streams.firstOrNull()
                    
                    if (stream != null) {
                        val headers = HashMap<String, String>().apply {
                            putAll(source.headers)
                            putAll(stream.headers)
                        }
                        playerController.prepare(
                            streamUrl = stream.url,
                            headers = headers,
                            startPositionMs = _uiState.value.initialPositionMs,
                            autoPlay = true
                        )
                        _uiState.value = _uiState.value.copy(
                            playbackSource = source,
                            rentalBlockedEpisode = null,
                            isRefreshing = false
                        )
                    } else {
                        if (episode.isSewa) {
                            playerController.pause()
                            _uiState.value = _uiState.value.copy(
                                rentalBlockedEpisode = episode,
                                isRefreshing = false
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Tidak ada stream video yang tersedia. Silakan coba lagi.",
                                isRefreshing = false
                            )
                        }
                    }
                } else {
                    if (episode.isSewa) {
                        playerController.pause()
                        _uiState.value = _uiState.value.copy(
                            rentalBlockedEpisode = episode,
                            isRefreshing = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = sourceResult.exceptionOrNull()?.localizedMessage ?: "Gagal memuat stream",
                            isRefreshing = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error: ${e.localizedMessage}",
                    isRefreshing = false
                )
            }
        }
    }

    fun selectSeason(seasonIndex: Int) {
        _uiState.value = _uiState.value.copy(currentSeasonIndex = seasonIndex)
    }

    suspend fun saveCurrentProgress() {
        val currentEp = _uiState.value.currentEpisode ?: return
        val detail = _uiState.value.detail ?: return
        val player = playerController.player

        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val playerDuration = player.duration.coerceAtLeast(0L)
        val durationMs = if (playerDuration > 0L) playerDuration else _uiState.value.prefilledDurationMs

        if (durationMs > 0L && positionMs > 0L) {
            val isCompleted = positionMs >= (durationMs * 0.95)
            val history = WatchHistoryEntity(
                dramaId = activeDramaId,
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

    fun pausePlayback() {
        playerController.pause()
    }

    fun release() {
        stopSubtitleSync()
        subtitleManager.clear()
        historyTrackingJob?.cancel()
        historyTrackingJob = null
        playerController.release()
    }

    override fun onCleared() {
        release()
        super.onCleared()
    }
}

fun formatQualityLabel(raw: String): String {
    val trimmed = raw.trim()
    return when {
        trimmed.endsWith("p", ignoreCase = true) -> {
            trimmed.dropLast(1) + "P"
        }
        trimmed.equals("auto", ignoreCase = true) -> "Auto"
        else -> trimmed.uppercase()
    }
}

fun sortQualitiesDescending(qualities: List<String>): List<String> {
    return qualities.sortedWith { a, b ->
        val weightA = getQualityWeight(a)
        val weightB = getQualityWeight(b)
        weightB.compareTo(weightA)
    }
}

private fun getQualityWeight(label: String): Int {
    val clean = label.trim().lowercase()
    if (clean.contains("4k") || clean.contains("2160")) return 2160
    if (clean.contains("2k") || clean.contains("1440")) return 1440
    if (clean.contains("1080")) return 1080
    if (clean.contains("720")) return 720
    if (clean.contains("540")) return 540
    if (clean.contains("480")) return 480
    if (clean.contains("360")) return 360
    if (clean.contains("240")) return 240
    val num = Regex("""\d+""").find(clean)?.value?.toIntOrNull()
    if (num != null) return num
    if (clean == "auto") return 0
    return -1
}

fun formatSubtitleLabel(lang: String?, label: String?, url: String? = null): String {
    val candidates = listOfNotNull(
        lang?.lowercase()?.trim(),
        label?.lowercase()?.trim(),
        url?.let { extractLangFromUrl(it) }
    )

    for (c in candidates) {
        val mapped = matchLanguage(c)
        if (mapped != null) return mapped
    }

    if (!label.isNullOrBlank() && !label.equals("sub", ignoreCase = true) && !label.equals("subtitle", ignoreCase = true)) {
        return label.trim()
    }

    if (!lang.isNullOrBlank() && !lang.equals("sub", ignoreCase = true)) {
        return lang.uppercase().trim()
    }

    return "Subtitle"
}

private fun extractLangFromUrl(url: String): String? {
    val lower = url.lowercase()
    if (lower.contains("indonesian") || lower.contains("indonesia")) return "id"
    if (lower.contains("english")) return "en"
    if (lower.contains("malay") || lower.contains("melayu")) return "ms"
    if (lower.contains("arabic")) return "ar"
    if (lower.contains("spanish") || lower.contains("espanol")) return "es"
    if (lower.contains("portuguese") || lower.contains("portugis")) return "pt"
    if (lower.contains("vietnamese") || lower.contains("vietnam")) return "vi"
    if (lower.contains("thai")) return "th"
    if (lower.contains("khmer")) return "km"
    if (lower.contains("chinese") || lower.contains("mandarin")) return "zh"
    if (lower.contains("korean")) return "ko"
    if (lower.contains("japanese")) return "ja"

    val filePattern = Regex("""[._-]([a-zA-Z]{2,3}(?:[-_][a-zA-Z]{2,4})?)\.(?:srt|vtt)""")
    filePattern.find(lower)?.groupValues?.get(1)?.let { return it }

    val pathPattern = Regex("""/(?:mul/)?([a-zA-Z]{2,3})/[^/]+\.(?:srt|vtt)""")
    pathPattern.find(lower)?.groupValues?.get(1)?.let { return it }

    return null
}

private fun matchLanguage(code: String): String? {
    val clean = code.trim().lowercase().replace('_', '-')
    return when {
        // Indonesian
        clean == "id" || clean == "in" || clean == "ind" || clean == "id-id" || clean == "in-id" ||
            clean.contains("indo") || clean == "bahasa" || clean == "bahasa indonesia" -> "Indonesian"

        // English
        clean == "en" || clean == "eng" || clean == "en-us" || clean == "en-gb" ||
            clean.contains("english") || clean.contains("inggris") -> "English"

        // Malay
        clean == "ms" || clean == "may" || clean == "msa" || clean == "ms-my" ||
            clean.contains("malay") || clean.contains("melayu") -> "Malay"

        // Arabic
        clean == "ar" || clean == "ara" || clean == "ar-sa" || clean == "ar-ae" ||
            clean.contains("arab") || clean.contains("عرب") -> "Arabic"

        // Chinese Simplified & Traditional
        clean == "zh-cn" || clean == "zh-hans" || clean.contains("sederhana") || clean.contains("simplified") -> "Mandarin (Sederhana)"
        clean == "zh-tw" || clean == "zh-hk" || clean == "zh-hant" || clean.contains("tradisional") || clean.contains("traditional") -> "Mandarin (Tradisional)"
        clean == "zh" || clean == "chi" || clean == "zho" || clean.contains("mandarin") || clean.contains("chinese") || clean.contains("中文") -> "Mandarin"

        // Thai
        clean == "th" || clean == "tha" || clean == "th-th" || clean.contains("thai") || clean.contains("ไทย") -> "Thai"

        // Vietnamese
        clean == "vi" || clean == "vie" || clean == "vi-vn" || clean.contains("viet") || clean.contains("tiếng việt") -> "Vietnamese"

        // Khmer
        clean == "km" || clean == "khm" || clean == "km-kh" || clean.contains("khmer") || clean.contains("kamboja") || clean.contains("ខេមរ") -> "Khmer"

        // Korean
        clean == "ko" || clean == "kor" || clean == "ko-kr" || clean.contains("korea") || clean.contains("한국") -> "Korean"

        // Japanese
        clean == "ja" || clean == "jp" || clean == "jpn" || clean == "ja-jp" || clean.contains("japan") || clean.contains("jepang") || clean.contains("日本") -> "Japanese"

        // Spanish
        clean == "es" || clean == "spa" || clean == "es-es" || clean == "es-la" || clean == "es-mx" ||
            clean.contains("span") || clean.contains("spanyol") || clean.contains("español") -> "Spanish"

        // Portuguese
        clean == "pt" || clean == "por" || clean == "pt-br" || clean == "pt-pt" ||
            clean.contains("portug") -> "Portuguese"

        // French
        clean == "fr" || clean == "fra" || clean == "fre" || clean == "fr-fr" ||
            clean.contains("french") || clean.contains("prancis") || clean.contains("français") -> "French"

        // German
        clean == "de" || clean == "deu" || clean == "ger" || clean == "de-de" ||
            clean.contains("german") || clean.contains("jerman") || clean.contains("deutsch") -> "German"

        // Russian
        clean == "ru" || clean == "rus" || clean == "ru-ru" || clean.contains("russ") || clean.contains("rusia") || clean.contains("рус") -> "Russian"

        // Hindi
        clean == "hi" || clean == "hin" || clean == "hi-in" || clean.contains("hindi") || clean.contains("हिन्द") -> "Hindi"

        // Filipino / Tagalog
        clean == "tl" || clean == "fil" || clean == "fil-ph" || clean == "tgl" || clean.contains("tagalog") || clean.contains("filipino") -> "Filipino"

        // Bengali
        clean == "bn" || clean == "ben" || clean.contains("bengali") || clean.contains("bangla") -> "Bengali"

        // Punjabi
        clean == "pa" || clean == "pan" || clean.contains("punjabi") -> "Punjabi"

        // Urdu
        clean == "ur" || clean == "urd" || clean.contains("urdu") -> "Urdu"

        // Turkish
        clean == "tr" || clean == "tur" || clean.contains("turk") -> "Turkish"

        // Italian
        clean == "it" || clean == "ita" || clean.contains("ital") -> "Italian"

        // Dutch
        clean == "nl" || clean == "nld" || clean == "dut" || clean.contains("dutch") || clean.contains("belanda") -> "Dutch"

        // Polish
        clean == "pl" || clean == "pol" || clean.contains("polish") || clean.contains("poland") -> "Polish"

        else -> null
    }
}
