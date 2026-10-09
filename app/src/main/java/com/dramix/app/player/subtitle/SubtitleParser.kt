package com.dramix.app.player.subtitle

import com.dramix.app.player.model.SubtitleCue

object SubtitleParser {
    
    fun parseVTT(content: String): List<SubtitleCue> {
        return parseSubtitleFormat(content, isVTT = true)
    }
    
    fun parseSRT(content: String): List<SubtitleCue> {
        return parseSubtitleFormat(content, isVTT = false)
    }
    
    fun parseAuto(content: String): List<SubtitleCue> {
        return when {
            content.trim().startsWith("WEBVTT") -> parseVTT(content)
            else -> parseSRT(content)
        }
    }
    
    private fun parseSubtitleFormat(content: String, isVTT: Boolean): List<SubtitleCue> {
        val subtitles = mutableListOf<SubtitleCue>()
        val lines = content.split("\n").map { it.trim() }
        
        var i = 0
        var subtitleId = 0
        
        if (isVTT && lines.isNotEmpty() && lines[0].startsWith("WEBVTT")) {
            i = 1
        }
        
        while (i < lines.size) {
            val line = lines[i].trim()
            
            if (line.isEmpty()) {
                i++
                continue
            }
            
            if (line.contains("-->")) {
                val (startMs, endMs) = parseTimeline(line)
                val textLines = mutableListOf<String>()
                
                i++
                while (i < lines.size && lines[i].isNotEmpty() && !lines[i].contains("-->")) {
                    textLines.add(lines[i].trim())
                    i++
                }
                
                if (textLines.isNotEmpty()) {
                    subtitles.add(
                        SubtitleCue(
                            id = "${++subtitleId}",
                            startTimeMs = startMs,
                            endTimeMs = endMs,
                            text = textLines.joinToString("\n")
                        )
                    )
                }
            } else {
                i++
            }
        }
        
        return subtitles
    }
    
    private fun parseTimeline(timelineStr: String): Pair<Long, Long> {
        val parts = timelineStr.split("-->")
        if (parts.size != 2) return 0L to 0L
        
        val start = parseTime(parts[0].trim().split(" ")[0])
        val end = parseTime(parts[1].trim().split(" ")[0])
        
        return start to end
    }
    
    private fun parseTime(timeStr: String): Long {
        val parts = timeStr.split(":")
        if (parts.size < 2) return 0L
        
        val hours = parts.getOrNull(0)?.toLongOrNull() ?: 0L
        val minutes = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        val secondsAndMs = parts.getOrNull(2)?.replace(",", ".") ?: "0.0"
        val secondsParts = secondsAndMs.split(".")
        val seconds = secondsParts.getOrNull(0)?.toLongOrNull() ?: 0L
        val millis = secondsParts.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLongOrNull() ?: 0L
        
        return (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + millis
    }
}
