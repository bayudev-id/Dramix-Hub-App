package com.dramix.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class LiveTvChannelMetaTest {

    @Test
    fun parses_nba_event_payload_correctly() {
        // Base64 of: {"v":1,"cc":"EV","cid":"1455","n":"Basketball: NBA [CH5]","d":"08 Okt 06:00 WIB - Indiana Pacers vs Minnesota Timberwolves","ns":"National Basketball Association 5","j":"ts","cn":"Events","lv":true,"mv":false,"pr":true}
        val encodedId = "bittvd_eyJ2IjoxLCJjYyI6IkVWIiwiY2lkIjoiMTQ1NSIsIm4iOiJCYXNrZXRiYWxsOiBOQkEgW0NINV0iLCJkIjoiMDggT2t0IDA2OjAwIFdJQiAtIEluZGlhbmEgUGFjZXJzIHZzIE1pbm5lc290YSBUaW1iZXJ3b2x2ZXMiLCJucyI6Ik5hdGlvbmFsIEJhc2tldGJhbGwgQXNzb2NpYXRpb24gNSIsImoiOiJ0cyIsImNuIjoiRXZlbnRzIiwibHYiOnRydWUsIm12IjpmYWxzZSwicHIiOnRydWV9"

        val videoItem = VideoItem(
            id = encodedId,
            title = "Basketball: NBA [CH5]",
            cover = "https://example.com/poster.jpg"
        )

        val meta = videoItem.toLiveTvMeta()

        assertTrue(meta.isEventMatch)
        assertEquals("bittv:EV:1455", meta.playbackEpisodeId)
        assertEquals("08 Okt 06:00 WIB", meta.matchSchedule)
        assertEquals("Indiana Pacers vs Minnesota Timberwolves", meta.matchTeams)
        assertEquals("Indiana Pacers", meta.team1)
        assertEquals("Minnesota Timberwolves", meta.team2)
        assertEquals("Basketball: NBA [CH5]", meta.title)
        assertEquals("National Basketball Association 5", meta.tournamentName)
        assertEquals("TS", meta.streamType)
        assertTrue(meta.isLive)
        assertTrue(meta.isPremium)
    }

    @Test
    fun parses_regular_channel_payload_correctly() {
        // Base64 of: {"v":1,"cc":"ID","cid":"986","n":"Sindo News","d":"News/Berita","ns":"MNC News","j":"drm_rcti_plus","cn":"Indonesia","lv":false,"mv":false,"pr":true}
        val encodedId = "bittvd_eyJ2IjoxLCJjYyI6IklEIiwiY2lkIjoiOTg2IiwibiI6IlNpbmRvIE5ld3MiLCJkIjoiTmV3cy9CZXJpdGEiLCJucyI6Ik1OQyBOZXdzIiwiaiI6ImRybV9yY3RpX3BsdXMiLCJjbiI6IkluZG9uZXNpYSIsImx2IjpmYWxzZSwibXYiOmZhbHNlLCJwciI6dHJ1ZX0"

        val videoItem = VideoItem(
            id = encodedId,
            title = "Sindo News",
            cover = "https://example.com/sindo.jpg"
        )

        val meta = videoItem.toLiveTvMeta()

        assertFalse(meta.isEventMatch)
        assertEquals("bittv:ID:986", meta.playbackEpisodeId)
        assertEquals("Sindo News", meta.title)
        assertEquals("News/Berita", meta.subName)
        assertEquals("RCTI+ DRM", meta.streamType)
        assertNull(meta.team1)
        assertNull(meta.team2)
        assertTrue(meta.isPremium)
    }

    @Test
    fun fallback_for_standard_item_without_bittvd() {
        val videoItem = VideoItem(
            id = "rcti_regular",
            title = "RCTI",
            score = "LIVE",
            views = "HD Siaran",
            episodeInfo = "Nasional"
        )

        val meta = videoItem.toLiveTvMeta()

        assertFalse(meta.isEventMatch)
        assertEquals("rcti_regular", meta.playbackEpisodeId)
        assertEquals("RCTI", meta.title)
        assertTrue(meta.isLive)
    }
}
