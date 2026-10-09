package com.dramix.app.player.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleParserAndManagerTest {

    @Test
    fun parseSRT_validContent_returnsParsedCues() {
        val srtContent = """
            1
            00:00:01,000 --> 00:00:04,000
            Hello world!

            2
            00:00:05,500 --> 00:00:08,000
            This is custom subtitle.
        """.trimIndent()

        val cues = SubtitleParser.parseSRT(srtContent)
        assertEquals(2, cues.size)
        assertEquals("Hello world!", cues[0].text)
        assertEquals(1000L, cues[0].startTimeMs)
        assertEquals(4000L, cues[0].endTimeMs)

        assertEquals("This is custom subtitle.", cues[1].text)
        assertEquals(5500L, cues[1].startTimeMs)
        assertEquals(8000L, cues[1].endTimeMs)
    }

    @Test
    fun parseVTT_validContent_returnsParsedCues() {
        val vttContent = """
            WEBVTT

            00:00:01.200 --> 00:00:03.500
            Line 1

            00:00:04.000 --> 00:00:07.000
            Line 2
        """.trimIndent()

        val cues = SubtitleParser.parseVTT(vttContent)
        assertEquals(2, cues.size)
        assertEquals("Line 1", cues[0].text)
        assertEquals(1200L, cues[0].startTimeMs)
        assertEquals(3500L, cues[0].endTimeMs)
    }

    @Test
    fun subtitleManager_tracksPositionAccurately() {
        val manager = SubtitleManager()
        val srtContent = """
            1
            00:00:01,000 --> 00:00:04,000
            Hello world!
        """.trimIndent()

        val cues = SubtitleParser.parseSRT(srtContent)
        manager.setSubtitles(cues)

        manager.updatePosition(500L)
        assertNull(manager.currentSubtitle.value)

        manager.updatePosition(2000L)
        assertNotNull(manager.currentSubtitle.value)
        assertEquals("Hello world!", manager.currentSubtitle.value?.text)

        manager.updatePosition(4500L)
        assertNull(manager.currentSubtitle.value)
    }
}
