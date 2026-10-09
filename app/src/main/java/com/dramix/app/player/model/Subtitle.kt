package com.dramix.app.player.model

data class SubtitleCue(
    val id: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)
