package com.dramix.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeTest {

    @Test
    fun `isSewa returns true when label is Sewa`() {
        val episode = Episode(
            id = "ep_1",
            title = "EP09: Sewa Konglo Jadi Suami",
            number = 9,
            label = "Sewa",
            tags = emptyList()
        )
        assertTrue(episode.isSewa)
    }

    @Test
    fun `isSewa returns true when label is lowercase sewa`() {
        val episode = Episode(
            id = "ep_2",
            title = "EP10: Sewa Konglo Jadi Suami",
            number = 10,
            label = "sewa",
            tags = emptyList()
        )
        assertTrue(episode.isSewa)
    }

    @Test
    fun `isSewa returns true when tags contain Sewa`() {
        val episode = Episode(
            id = "ep_3",
            title = "EP11",
            number = 11,
            label = null,
            tags = listOf("Sewa")
        )
        assertTrue(episode.isSewa)
    }

    @Test
    fun `isSewa returns true when tags contain rent`() {
        val episode = Episode(
            id = "ep_4",
            title = "EP12",
            number = 12,
            label = "",
            tags = listOf("Rent")
        )
        assertTrue(episode.isSewa)
    }

    @Test
    fun `isSewa returns false for free episode without label or tag`() {
        val episode = Episode(
            id = "ep_free",
            title = "EP01",
            number = 1,
            label = "",
            tags = emptyList()
        )
        assertFalse(episode.isSewa)
    }

    @Test
    fun `isSewa returns false for standard VIP episode`() {
        val episode = Episode(
            id = "ep_vip",
            title = "EP05",
            number = 5,
            isVip = true,
            label = "VIP",
            tags = listOf("VIP")
        )
        assertFalse(episode.isSewa)
    }
}
