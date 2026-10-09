package com.dramix.app.player.controller

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.dramix.app.domain.model.Subtitle
import com.dramix.app.player.engine.HeaderInjectingDataSourceFactory
import com.dramix.app.player.model.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class DramixPlayerController(
    val player: ExoPlayer,
    private val headerDataSourceFactory: HeaderInjectingDataSourceFactory,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var progressTrackingJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            val stateName = when(state) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN"
            }
            android.util.Log.d("PlayerController", "State changed: $stateName")
            updateState(state)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            android.util.Log.d("PlayerController", "IsPlaying changed: $isPlaying")
            if (isPlaying) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
            updateState(player.playbackState)
        }

        override fun onPlayerError(error: PlaybackException) {
            android.util.Log.e("PlayerController", "Player error: ${error.message}", error)
            stopProgressTracker()
            _playbackState.value = PlaybackState.Error(
                message = error.localizedMessage ?: "Playback error: ${error.errorCodeName}",
                cause = error
            )
        }
    }

    init {
        player.addListener(playerListener)
    }

    fun prepare(
        streamUrl: String,
        headers: Map<String, String> = emptyMap(),
        startPositionMs: Long = 0L,
        autoPlay: Boolean = true,
        subtitles: List<Subtitle> = emptyList(),
        preferredSubtitleLang: String? = null
    ) {
        try {
            headerDataSourceFactory.setHeaders(headers)

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(Uri.parse(streamUrl))

            if (subtitles.isNotEmpty()) {
                val subtitleConfigurations = subtitles.mapNotNull { sub ->
                    if (sub.url.isBlank()) return@mapNotNull null
                    val uri = Uri.parse(sub.url)
                    val mimeType = when {
                        sub.url.contains(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                        sub.url.contains(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                        sub.url.contains(".ssa", ignoreCase = true) || sub.url.contains(".ass", ignoreCase = true) -> MimeTypes.TEXT_SSA
                        sub.url.contains(".ttml", ignoreCase = true) -> MimeTypes.APPLICATION_TTML
                        else -> MimeTypes.APPLICATION_SUBRIP
                    }
                    val lang = sub.lang?.lowercase()?.trim() ?: "und"
                    val isDefault = preferredSubtitleLang?.equals(lang, ignoreCase = true) == true ||
                        (preferredSubtitleLang == null && (lang == "id" || sub.label?.contains("indo", ignoreCase = true) == true))

                    MediaItem.SubtitleConfiguration.Builder(uri)
                        .setMimeType(mimeType)
                        .setLanguage(lang)
                        .setLabel(sub.label ?: sub.lang ?: "Subtitle")
                        .setSelectionFlags(if (isDefault) C.SELECTION_FLAG_DEFAULT else 0)
                        .build()
                }
                mediaItemBuilder.setSubtitleConfigurations(subtitleConfigurations)
            }

            val mediaItem = mediaItemBuilder.build()
            player.setMediaItem(mediaItem, /* resetPosition = */ true)
            player.seekTo(startPositionMs.coerceAtLeast(0L))

            // Subtitle track selection
            val trackParamsBuilder = player.trackSelectionParameters.buildUpon()
            if (!preferredSubtitleLang.isNullOrBlank() && !preferredSubtitleLang.equals("off", ignoreCase = true)) {
                trackParamsBuilder
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setPreferredTextLanguage(preferredSubtitleLang.lowercase())
            } else if (preferredSubtitleLang?.equals("off", ignoreCase = true) == true) {
                trackParamsBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else if (subtitles.any { it.lang?.equals("id", ignoreCase = true) == true }) {
                trackParamsBuilder
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setPreferredTextLanguage("id")
            }
            player.trackSelectionParameters = trackParamsBuilder.build()

            player.prepare()
            player.playWhenReady = autoPlay
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.Error(e.localizedMessage ?: "Gagal memuat siaran")
        }
    }

    fun setSubtitleLanguage(languageCode: String?) {
        val builder = player.trackSelectionParameters.buildUpon()
        if (languageCode.isNullOrBlank() || languageCode.equals("off", ignoreCase = true)) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            builder
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setPreferredTextLanguage(languageCode.lowercase())
        }
        player.trackSelectionParameters = builder.build()
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed.coerceIn(0.25f, 3.0f))
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceAtLeast(0L)
        player.seekTo(target)
        val duration = player.duration.coerceAtLeast(0L)
        _playbackState.value = PlaybackState.Ready(
            isPlaying = player.isPlaying,
            durationMs = duration,
            currentPositionMs = target
        )
    }

    fun seekBy(offsetMs: Long) {
        val current = player.currentPosition.coerceAtLeast(0L)
        val duration = player.duration.coerceAtLeast(0L)
        val target = if (duration > 0L) {
            (current + offsetMs).coerceIn(0L, duration)
        } else {
            (current + offsetMs).coerceAtLeast(0L)
        }
        seekTo(target)
    }

    fun release() {
        stopProgressTracker()
        player.removeListener(playerListener)
        player.stop()
        player.release()
        _playbackState.value = PlaybackState.Idle
    }

    private fun updateState(playerState: Int) {
        when (playerState) {
            Player.STATE_IDLE -> {
                _playbackState.value = PlaybackState.Idle
            }
            Player.STATE_BUFFERING -> {
                _playbackState.value = PlaybackState.Buffering
            }
            Player.STATE_READY -> {
                _playbackState.value = PlaybackState.Ready(
                    isPlaying = player.isPlaying,
                    durationMs = player.duration.coerceAtLeast(0L),
                    currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                )
            }
            Player.STATE_ENDED -> {
                stopProgressTracker()
                _playbackState.value = PlaybackState.Ended
            }
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressTrackingJob = scope.launch {
            while (isActive && player.isPlaying) {
                if (player.playbackState == Player.STATE_READY) {
                    _playbackState.value = PlaybackState.Ready(
                        isPlaying = true,
                        durationMs = player.duration.coerceAtLeast(0L),
                        currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                    )
                }
                delay(500L) // Poll every 500ms during active playback
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }
}