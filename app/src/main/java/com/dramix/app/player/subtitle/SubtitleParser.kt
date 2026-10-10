package com.dramix.app.player.subtitle

import com.dramix.app.player.model.SubtitleCue

object SubtitleParser {

    private val assOverrideTagRegex = Regex("""\{[^}]*\}""")
    
    fun parseVTT(content: String): List<SubtitleCue> {
        return parseSubtitleFormat(content, isVTT = true)
    }
    
    fun parseSRT(content: String): List<SubtitleCue> {
        return parseSubtitleFormat(content, isVTT = false)
    }

    fun parseASS(content: String): List<SubtitleCue> {
        val subtitles = mutableListOf<SubtitleCue>()
        val lines = content.split("\n").map { it.trim() }

        var inEvents = false
        var formatFields: List<String> = emptyList()
        var startIndex = 1
        var endIndex = 2
        var textIndex = 9
        var subtitleId = 0

        for (line in lines) {
            if (line.startsWith("[Events]", ignoreCase = true)) {
                inEvents = true
                continue
            }
            if (inEvents && line.startsWith("[") && line.endsWith("]")) {
                inEvents = false
                continue
            }

            if (line.startsWith("Format:", ignoreCase = true)) {
                val header = line.substringAfter(":").trim()
                formatFields = header.split(",").map { it.trim().lowercase() }
                val sIdx = formatFields.indexOf("start")
                val eIdx = formatFields.indexOf("end")
                val tIdx = formatFields.indexOf("text")
                if (sIdx != -1) startIndex = sIdx
                if (eIdx != -1) endIndex = eIdx
                textIndex = if (tIdx != -1) tIdx else (formatFields.size - 1).coerceAtLeast(0)
                continue
            }

            if (line.startsWith("Dialogue:", ignoreCase = true)) {
                val payload = line.substringAfter(":").trim()
                val limit = if (formatFields.isNotEmpty()) formatFields.size else 10

                val tokens = mutableListOf<String>()
                var remainder = payload
                for (i in 0 until (limit - 1)) {
                    val commaIdx = remainder.indexOf(',')
                    if (commaIdx == -1) break
                    tokens.add(remainder.substring(0, commaIdx).trim())
                    remainder = remainder.substring(commaIdx + 1)
                }
                tokens.add(remainder.trim())

                if (tokens.size > startIndex && tokens.size > endIndex) {
                    val startMs = parseAssTime(tokens[startIndex])
                    val endMs = parseAssTime(tokens[endIndex])

                    val rawText = if (tokens.size > textIndex) tokens[textIndex] else tokens.last()
                    val cleanText = cleanAssText(rawText)

                    if (cleanText.isNotBlank()) {
                        subtitles.add(
                            SubtitleCue(
                                id = "${++subtitleId}",
                                startTimeMs = startMs,
                                endTimeMs = endMs,
                                text = cleanText
                            )
                        )
                    }
                }
            }
        }

        return subtitles
    }

    private fun cleanAssText(rawText: String): String {
        return rawText
            .replace(assOverrideTagRegex, "")
            .replace("\\N", "\n")
            .replace("\\n", "\n")
            .replace("\\h", " ")
            .trim()
    }

    private fun parseAssTime(timeStr: String): Long {
        val parts = timeStr.trim().split(":")
        if (parts.size < 2) return 0L

        val hours = parts.getOrNull(0)?.toLongOrNull() ?: 0L
        val minutes = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        val secondsAndMs = parts.getOrNull(2)?.replace(",", ".") ?: "0.0"
        val secondsParts = secondsAndMs.split(".")
        val seconds = secondsParts.getOrNull(0)?.toLongOrNull() ?: 0L
        val fractionStr = secondsParts.getOrNull(1) ?: "0"
        val millis = fractionStr.padEnd(3, '0').take(3).toLongOrNull() ?: 0L

        return (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + millis
    }
    
    fun parseAuto(content: String): List<SubtitleCue> {
        val trimmed = content.trim()
        return when {
            trimmed.startsWith("WEBVTT") || trimmed.contains("WEBVTT") -> parseVTT(content)
            trimmed.contains("[Events]") || trimmed.contains("Dialogue:") || trimmed.contains("[Script Info]") -> parseASS(content)
            else -> {
                val srtCues = parseSRT(content)
                if (srtCues.isNotEmpty()) srtCues else parseASS(content)
            }
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
