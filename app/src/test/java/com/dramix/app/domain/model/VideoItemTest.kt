package com.dramix.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoItemTest {

    @Test
    fun `isSewa returns true when source is WeTV and tag contains Sewa`() {
        val item = VideoItem(
            id = "item1",
            title = "Drama Sewa",
            source = "WeTV",
            tags = listOf("Total 70 EP", "Sewa")
        )
        assertTrue(item.isSewa)
    }

    @Test
    fun `isSewa returns true when source is lowercase wetv and tag is lowercase sewa`() {
        val item = VideoItem(
            id = "item2",
            title = "Drama Sewa",
            source = "wetv",
            tags = listOf("sewa")
        )
        assertTrue(item.isSewa)
    }

    @Test
    fun `isSewa returns true when source is null but tag has Sewa`() {
        val item = VideoItem(
            id = "item3",
            title = "Drama Sewa",
            source = null,
            tags = listOf("Sewa")
        )
        assertTrue(item.isSewa)
    }

    @Test
    fun `isSewa returns true when tag has Rent`() {
        val item = VideoItem(
            id = "item4",
            title = "Drama Rent",
            source = "WeTV",
            tags = listOf("Rent")
        )
        assertTrue(item.isSewa)
    }

    @Test
    fun `isSewa returns false when source is different provider`() {
        val item = VideoItem(
            id = "item5",
            title = "Drama Other",
            source = "MovieBox",
            tags = listOf("Sewa")
        )
        assertFalse(item.isSewa)
    }

    @Test
    fun `isSewa returns false when tags do not contain sewa or rent`() {
        val item = VideoItem(
            id = "item6",
            title = "Drama VIP",
            source = "WeTV",
            tags = listOf("VIP", "Romance", "Total 24 EP")
        )
        assertFalse(item.isSewa)
    }

    @Test
    fun `isSewa returns false when tags list is empty`() {
        val item = VideoItem(
            id = "item7",
            title = "Drama Plain",
            source = "WeTV",
            tags = emptyList()
        )
        assertFalse(item.isSewa)
    }
}
