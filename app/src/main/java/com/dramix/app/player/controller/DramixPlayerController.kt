package com.dramix.app.player.controller

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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
            updateState(state)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
            updateState(player.playbackState)
        }

        override fun onPlayerError(error: PlaybackException) {
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
        autoPlay: Boolean = true
    ) {
        headerDataSourceFactory.setHeaders(headers)

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(streamUrl))
            .build()

        player.setMediaItem(mediaItem)
        if (startPositionMs > 0L) {
            player.seekTo(startPositionMs)
        }
        player.prepare()
        player.playWhenReady = autoPlay
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        updateState(player.playbackState)
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
