package com.dramix.app.player.subtitle

import com.dramix.app.player.model.SubtitleCue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SubtitleManager {
    
    private var subtitles: List<SubtitleCue> = emptyList()
    private val _currentSubtitle = MutableStateFlow<SubtitleCue?>(null)
    val currentSubtitle: StateFlow<SubtitleCue?> = _currentSubtitle.asStateFlow()
    
    fun setSubtitles(newSubtitles: List<SubtitleCue>) {
        subtitles = newSubtitles.sortedBy { it.startTimeMs }
        _currentSubtitle.value = null
    }
    
    fun updatePosition(positionMs: Long) {
        val active = subtitles.firstOrNull { sub ->
            positionMs in sub.startTimeMs..sub.endTimeMs
        }
        
        if (_currentSubtitle.value != active) {
            _currentSubtitle.value = active
        }
    }
    
    fun clear() {
        subtitles = emptyList()
        _currentSubtitle.value = null
    }
}
