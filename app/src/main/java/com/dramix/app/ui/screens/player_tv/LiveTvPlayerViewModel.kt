package com.dramix.app.ui.screens.player_tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import com.dramix.app.domain.model.Category
import com.dramix.app.domain.model.PlaybackSource
import com.dramix.app.domain.model.VideoItem
import com.dramix.app.domain.repository.CatalogRepository
import com.dramix.app.player.controller.DramixPlayerController
import com.dramix.app.player.engine.PlayerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveTvUiState(
    val isLoadingFeed: Boolean = true,
    val isLoadingStream: Boolean = false,
    val providerId: String = "CineTv",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String = "all",
    val channels: List<VideoItem> = emptyList(),
    val selectedChannel: VideoItem? = null,
    val playbackSource: PlaybackSource? = null,
    val isStreamError: Boolean = false,
    val errorMessage: String? = null
)

class LiveTvPlayerViewModel(
    initialProviderId: String? = null,
    private val initialChannelId: String? = null,
    private val catalogRepository: CatalogRepository,
    playerFactory: PlayerFactory,
    enablePlayerCache: Boolean = false
) : ViewModel() {

    private val playerPair = playerFactory.createPlayer(enableCache = enablePlayerCache)
    val playerController: DramixPlayerController = DramixPlayerController(
        player = playerPair.first.apply {
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        },
        headerDataSourceFactory = playerPair.second,
        scope = viewModelScope
    )

    private val _uiState = MutableStateFlow(
        LiveTvUiState(providerId = initialProviderId ?: "CineTv")
    )
    val uiState: StateFlow<LiveTvUiState> = _uiState.asStateFlow()

    init {
        loadLiveTvFeed(initialProviderId)
    }

    fun loadLiveTvFeed(provId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingFeed = true, errorMessage = null)

            val effectiveProviderId = if (provId.isNullOrBlank()) {
                val providers = catalogRepository.getProviders().getOrNull() ?: emptyList()
                providers.firstOrNull { it.contentType == "live_tv" }?.id ?: "CineTv"
            } else {
                provId
            }

            _uiState.value = _uiState.value.copy(providerId = effectiveProviderId)

            // 1. Load Categories
            val categoriesResult = catalogRepository.getCategories(effectiveProviderId)
            val fetchedCategories = categoriesResult.getOrNull() ?: emptyList()
            val categories = if (fetchedCategories.isEmpty()) {
                listOf(Category(id = "all", name = "Semua"))
            } else {
                fetchedCategories
            }

            // Prefer "ID" (Indonesia) if available, otherwise first category
            val initialCategoryId = categories.find { it.id.equals("ID", ignoreCase = true) }?.id
                ?: categories.first().id

            _uiState.value = _uiState.value.copy(
                categories = categories,
                selectedCategoryId = initialCategoryId
            )

            // 2. Load Channels for the selected category
            loadChannelsForCategory(effectiveProviderId, initialCategoryId)
        }
    }

    fun selectCategory(category: Category) {
        if (_uiState.value.selectedCategoryId == category.id) return

        _uiState.value = _uiState.value.copy(selectedCategoryId = category.id)
        viewModelScope.launch {
            loadChannelsForCategory(_uiState.value.providerId, category.id)
        }
    }

    private suspend fun loadChannelsForCategory(providerId: String, categoryId: String) {
        val videosResult = catalogRepository.getVideos(providerId, categoryId, page = 1)
        val channelList = videosResult.getOrNull() ?: emptyList()

        _uiState.value = _uiState.value.copy(
            channels = channelList,
            isLoadingFeed = false
        )

        // Select initial channel if none selected yet or if switching list
        val targetChannel = if (!initialChannelId.isNullOrBlank() && _uiState.value.selectedChannel == null) {
            channelList.find { it.id == initialChannelId } ?: channelList.firstOrNull()
        } else if (_uiState.value.selectedChannel == null) {
            channelList.firstOrNull()
        } else {
            // Keep current selected channel if it exists in new list, else select first
            channelList.find { it.id == _uiState.value.selectedChannel?.id } ?: channelList.firstOrNull()
        }

        if (targetChannel != null && targetChannel.id != _uiState.value.selectedChannel?.id) {
            playChannel(targetChannel)
        }
    }

    fun playChannel(channel: VideoItem) {
        _uiState.value = _uiState.value.copy(
            selectedChannel = channel,
            isLoadingStream = true,
            isStreamError = false,
            errorMessage = null
        )

        viewModelScope.launch {
            val sourceResult = catalogRepository.getPlaybackSource(
                modelId = _uiState.value.providerId,
                episodeId = channel.id,
                id = channel.id
            )

            if (sourceResult.isSuccess) {
                val source = sourceResult.getOrThrow()
                val stream = source.streams.firstOrNull()

                if (stream != null) {
                    val headers = HashMap<String, String>().apply {
                        putAll(source.headers)
                        putAll(stream.headers)
                    }

                    // Seamless stream switch
                    playerController.prepare(
                        streamUrl = stream.url,
                        headers = headers,
                        startPositionMs = 0L,
                        autoPlay = true
                    )

                    _uiState.value = _uiState.value.copy(
                        playbackSource = source,
                        isLoadingStream = false,
                        isStreamError = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingStream = false,
                        isStreamError = true,
                        errorMessage = "Aliran siaran tidak tersedia"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingStream = false,
                    isStreamError = true,
                    errorMessage = sourceResult.exceptionOrNull()?.localizedMessage ?: "Sinyal siaran terputus"
                )
            }
        }
    }

    fun retryCurrentChannel() {
        val currentChannel = _uiState.value.selectedChannel ?: return
        playChannel(currentChannel)
    }

    fun pausePlayback() {
        playerController.pause()
    }

    fun release() {
        playerController.release()
    }

    override fun onCleared() {
        release()
        super.onCleared()
    }
}
