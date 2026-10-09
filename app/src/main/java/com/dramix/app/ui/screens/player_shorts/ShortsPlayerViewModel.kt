package com.dramix.app.ui.screens.player_shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dramix.app.core.database.dao.BookmarkDao
import com.dramix.app.core.database.dao.WatchHistoryDao
import com.dramix.app.core.database.entity.BookmarkEntity
import com.dramix.app.core.database.entity.WatchHistoryEntity
import com.dramix.app.domain.manager.EntitlementManager
import com.dramix.app.domain.manager.PlaybackAccess
import com.dramix.app.domain.model.DramaDetail
import com.dramix.app.domain.model.Episode
import com.dramix.app.domain.model.PlaybackSource
import com.dramix.app.domain.repository.CatalogRepository
import com.dramix.app.player.controller.DramixPlayerController
import com.dramix.app.player.download.DownloadTracker
import com.dramix.app.player.engine.PlayerFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShortsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val providerId: String = "freereels",
    val dramaId: String = "",
    val detail: DramaDetail? = null,
    val episodes: List<Episode> = emptyList(),
    val currentEpisodeIndex: Int = 0,
    val currentPlaybackSource: PlaybackSource? = null,
    val isBookmarked: Boolean = false,
    val showDetailSheet: Boolean = false,
    val showLicenseGate: Boolean = false,
    val lockedEpisodeNumber: Int = 1,
    val rentalBlockedEpisode: Episode? = null,
    val errorMessage: String? = null
)

class ShortsPlayerViewModel(
    initialProviderId: String? = null,
    initialDramaId: String? = null,
    private val catalogRepository: CatalogRepository,
    private val watchHistoryDao: WatchHistoryDao,
    private val bookmarkDao: BookmarkDao,
    private val entitlementManager: EntitlementManager,
    playerFactory: PlayerFactory,
    enablePlayerCache: Boolean = true,
    private val downloadTracker: DownloadTracker? = null
) : ViewModel() {

    private val playerPair = playerFactory.createPlayer(enableCache = enablePlayerCache)
    val playerController: DramixPlayerController = DramixPlayerController(
        player = playerPair.first,
        headerDataSourceFactory = playerPair.second,
        scope = viewModelScope
    )

    private val _uiState = MutableStateFlow(
        ShortsUiState(
            providerId = initialProviderId ?: "freereels",
            dramaId = initialDramaId ?: ""
        )
    )
    val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()

    private var prebufferJob: Job? = null

    init {
        loadShortDramaFeed(initialProviderId, initialDramaId)
    }

    fun loadShortDramaFeed(provId: String?, dramId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val effectiveProviderId = if (provId.isNullOrBlank()) {
                val providers = catalogRepository.getProviders().getOrNull() ?: emptyList()
                providers.firstOrNull { it.contentType == "short_drama" }?.id ?: "freereels"
            } else {
                provId
            }

            val effectiveDramaId = if (dramId.isNullOrBlank()) {
                val categories = catalogRepository.getCategories(effectiveProviderId).getOrNull()
                val catId = categories?.firstOrNull()?.id ?: "all"
                val videos = catalogRepository.getVideos(effectiveProviderId, catId, 1).getOrNull() ?: emptyList()
                videos.firstOrNull()?.id ?: ""
            } else {
                dramId
            }

            if (effectiveDramaId.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Tidak ada drama pendek yang ditemukan"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                providerId = effectiveProviderId,
                dramaId = effectiveDramaId
            )

            observeBookmarkStatus(effectiveDramaId, effectiveProviderId)
            loadDramaDetail(effectiveProviderId, effectiveDramaId)
        }
    }

    private fun observeBookmarkStatus(dramaId: String, providerId: String) {
        viewModelScope.launch {
            bookmarkDao.isBookmarked(dramaId, providerId).collect { bookmarked ->
                _uiState.value = _uiState.value.copy(isBookmarked = bookmarked)
            }
        }
    }

    private suspend fun loadDramaDetail(providerId: String, dramaId: String) {
        val detailResult = catalogRepository.getDramaDetail(providerId, dramaId)
        if (detailResult.isFailure) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = detailResult.exceptionOrNull()?.localizedMessage ?: "Gagal memuat detail drama"
            )
            return
        }

        val detail = detailResult.getOrThrow()
        val allEpisodes = detail.seasons.firstOrNull()?.episodes
            ?: (1..detail.totalEpisodes.coerceAtLeast(1)).map { num ->
                Episode(id = "ep-$num", title = "Episode $num", number = num)
            }

        _uiState.value = _uiState.value.copy(
            detail = detail,
            episodes = allEpisodes,
            isLoading = false
        )

        // Start playback on initial episode (index 0)
        onEpisodeSelected(0)
    }

    fun onEpisodeSelected(index: Int) {
        val episodes = _uiState.value.episodes
        if (index !in episodes.indices) return

        val targetEpisode = episodes[index]
        if (!targetEpisode.isSewa) {
            val access = entitlementManager.canPlayEpisode(targetEpisode.number, targetEpisode.isVip)
            if (access is PlaybackAccess.AccessDenied) {
                playerController.pause()
                _uiState.value = _uiState.value.copy(
                    showLicenseGate = true,
                    lockedEpisodeNumber = targetEpisode.number
                )
                return
            }
        }

        // Save progress for previous episode before switching index
        val previousIndex = _uiState.value.currentEpisodeIndex
        if (previousIndex in episodes.indices && previousIndex != index) {
            val prevEp = episodes[previousIndex]
            val detail = _uiState.value.detail
            val player = playerController.player
            val positionMs = player.currentPosition.coerceAtLeast(0L)
            val durationMs = player.duration.coerceAtLeast(0L)
            if (detail != null && durationMs > 0L && positionMs > 0L) {
                viewModelScope.launch {
                    val history = WatchHistoryEntity(
                        dramaId = _uiState.value.dramaId,
                        providerId = _uiState.value.providerId,
                        dramaTitle = detail.title,
                        dramaPoster = detail.cover,
                        episodeNumber = prevEp.number,
                        episodeTitle = prevEp.title,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        isCompleted = positionMs >= (durationMs * 0.95),
                        updatedAt = System.currentTimeMillis()
                    )
                    watchHistoryDao.insertOrUpdateWatchHistory(history)
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            currentEpisodeIndex = index,
            showLicenseGate = false,
            showDetailSheet = false,
            errorMessage = null
        )

        viewModelScope.launch {
            val sourceResult = catalogRepository.getPlaybackSource(
                modelId = _uiState.value.providerId,
                episodeId = targetEpisode.id,
                id = _uiState.value.dramaId
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
                        startPositionMs = 0L,
                        autoPlay = true
                    )

                    _uiState.value = _uiState.value.copy(
                        currentPlaybackSource = source,
                        rentalBlockedEpisode = null,
                        errorMessage = null
                    )

                    // Pre-buffer next episode (n+1)
                    prebufferNextEpisode(index + 1)
                } else {
                    // Stream kosong / tidak ada URL stream
                    if (targetEpisode.isSewa) {
                        playerController.pause()
                        _uiState.value = _uiState.value.copy(
                            rentalBlockedEpisode = targetEpisode,
                            errorMessage = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Tidak ada stream video yang tersedia untuk episode ini"
                        )
                    }
                }
            } else {
                if (targetEpisode.isSewa) {
                    playerController.pause()
                    _uiState.value = _uiState.value.copy(
                        rentalBlockedEpisode = targetEpisode,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = sourceResult.exceptionOrNull()?.localizedMessage ?: "Gagal memuat stream"
                    )
                }
            }
        }
    }

    fun dismissRentalGate() {
        _uiState.value = _uiState.value.copy(rentalBlockedEpisode = null)
    }

    fun dismissErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun refreshCurrentEpisode() {
        val index = _uiState.value.currentEpisodeIndex
        val episodes = _uiState.value.episodes
        if (index !in episodes.indices) return
        if (_uiState.value.isRefreshing) return

        val targetEpisode = episodes[index]
        _uiState.value = _uiState.value.copy(
            isRefreshing = true,
            errorMessage = null,
            rentalBlockedEpisode = null
        )

        viewModelScope.launch {
            val sourceResult = catalogRepository.getPlaybackSource(
                modelId = _uiState.value.providerId,
                episodeId = targetEpisode.id,
                id = _uiState.value.dramaId
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
                        startPositionMs = 0L,
                        autoPlay = true
                    )

                    _uiState.value = _uiState.value.copy(
                        currentPlaybackSource = source,
                        rentalBlockedEpisode = null,
                        errorMessage = null,
                        isRefreshing = false
                    )

                    prebufferNextEpisode(index + 1)
                } else {
                    if (targetEpisode.isSewa) {
                        playerController.pause()
                        _uiState.value = _uiState.value.copy(
                            rentalBlockedEpisode = targetEpisode,
                            errorMessage = null,
                            isRefreshing = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Tidak ada stream video yang tersedia untuk episode ini",
                            isRefreshing = false
                        )
                    }
                }
            } else {
                if (targetEpisode.isSewa) {
                    playerController.pause()
                    _uiState.value = _uiState.value.copy(
                        rentalBlockedEpisode = targetEpisode,
                        errorMessage = null,
                        isRefreshing = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = sourceResult.exceptionOrNull()?.localizedMessage ?: "Gagal memuat stream",
                        isRefreshing = false
                    )
                }
            }
        }
    }

    private fun prebufferNextEpisode(nextIndex: Int) {
        val episodes = _uiState.value.episodes
        if (nextIndex !in episodes.indices) return

        val nextEpisode = episodes[nextIndex]
        val access = entitlementManager.canPlayEpisode(nextEpisode.number, nextEpisode.isVip)
        if (access is PlaybackAccess.AccessDenied) return

        prebufferJob?.cancel()
        prebufferJob = viewModelScope.launch {
            // Fetch source metadata in background so CDN headers and URL are ready
            catalogRepository.getPlaybackSource(
                modelId = _uiState.value.providerId,
                episodeId = nextEpisode.id,
                id = _uiState.value.dramaId
            )
        }
    }

    fun toggleBookmark() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            if (_uiState.value.isBookmarked) {
                bookmarkDao.deleteBookmark(_uiState.value.dramaId, _uiState.value.providerId)
            } else {
                val bookmark = BookmarkEntity(
                    dramaId = _uiState.value.dramaId,
                    providerId = _uiState.value.providerId,
                    title = detail.title,
                    posterUrl = detail.cover,
                    contentType = "short_drama",
                    rating = detail.score,
                    totalEpisodes = detail.totalEpisodes
                )
                bookmarkDao.insertBookmark(bookmark)
            }
        }
    }

    fun openDetailSheet() {
        _uiState.value = _uiState.value.copy(showDetailSheet = true)
    }

    fun closeDetailSheet() {
        _uiState.value = _uiState.value.copy(showDetailSheet = false)
    }

    fun pausePlayback() {
        playerController.pause()
    }

    fun dismissLicenseGate() {
        _uiState.value = _uiState.value.copy(showLicenseGate = false)
    }

    suspend fun saveCurrentProgress() {
        val episodes = _uiState.value.episodes
        val index = _uiState.value.currentEpisodeIndex
        if (index !in episodes.indices) return

        val currentEp = episodes[index]
        val detail = _uiState.value.detail ?: return
        val player = playerController.player

        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val durationMs = player.duration.coerceAtLeast(0L)

        if (durationMs > 0L && positionMs > 0L) {
            val history = WatchHistoryEntity(
                dramaId = _uiState.value.dramaId,
                providerId = _uiState.value.providerId,
                dramaTitle = detail.title,
                dramaPoster = detail.cover,
                episodeNumber = currentEp.number,
                episodeTitle = currentEp.title,
                positionMs = positionMs,
                durationMs = durationMs,
                isCompleted = positionMs >= (durationMs * 0.95),
                updatedAt = System.currentTimeMillis()
            )
            watchHistoryDao.insertOrUpdateWatchHistory(history)
        }
    }

    fun downloadCurrentEpisode() {
        val detail = _uiState.value.detail ?: return
        val episodes = _uiState.value.episodes
        val index = _uiState.value.currentEpisodeIndex
        if (index !in episodes.indices) return
        val ep = episodes[index]
        val source = _uiState.value.currentPlaybackSource ?: return
        val stream = source.streams.firstOrNull() ?: return

        val headers = HashMap<String, String>().apply {
            putAll(source.headers)
            putAll(stream.headers)
        }

        downloadTracker?.startDownload(
            dramaId = _uiState.value.dramaId,
            providerId = _uiState.value.providerId,
            dramaTitle = detail.title,
            episodeNumber = ep.number,
            episodeTitle = ep.title,
            streamUrl = stream.url,
            mediaId = ep.id,
            headers = headers
        )
    }

    fun release() {
        prebufferJob?.cancel()
        prebufferJob = null
        playerController.release()
    }

    override fun onCleared() {
        release()
        super.onCleared()
    }
}
