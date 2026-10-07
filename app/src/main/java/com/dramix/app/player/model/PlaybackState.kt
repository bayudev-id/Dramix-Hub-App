package com.dramix.app.player.model

sealed interface PlaybackState {
    data object Idle : PlaybackState
    data object Buffering : PlaybackState
    data class Ready(
        val isPlaying: Boolean = false,
        val durationMs: Long = 0L,
        val currentPositionMs: Long = 0L
    ) : PlaybackState
    data object Ended : PlaybackState
    data class Error(val message: String, val cause: Throwable? = null) : PlaybackState
}
