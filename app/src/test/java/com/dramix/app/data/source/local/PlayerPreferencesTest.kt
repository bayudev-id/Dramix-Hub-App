package com.dramix.app.data.source.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dramix.app.ui.screens.player_vod.SubtitleStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PlayerPreferencesTest {

    private lateinit var context: Context
    private lateinit var preferences: PlayerPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("dramix_player_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        preferences = PlayerPreferences(context)
    }

    @Test
    fun defaultValues_areSensible() {
        assertEquals("100%", preferences.getVideoZoom())
        assertEquals(1.0f, preferences.getPlaybackSpeed(), 0.01f)
        assertFalse(preferences.isAutoNext())
        val portraitStyle = preferences.getSubtitleStyle(isFullscreen = false)
        assertEquals("Arial", portraitStyle.fontFamily)
        assertEquals("Medium", portraitStyle.outlineStyle)
        assertEquals(14, portraitStyle.fontSizePx)
        assertEquals(10, portraitStyle.positionPercent)
        assertEquals(0, portraitStyle.backgroundOpacityPercent)
        assertEquals(0, portraitStyle.lineSpacingPx)
        assertEquals(0, portraitStyle.backgroundPaddingPx)

        val fsStyle = preferences.getSubtitleStyle(isFullscreen = true)
        assertEquals("Arial", fsStyle.fontFamily)
        assertEquals("Medium", fsStyle.outlineStyle)
        assertEquals(20, fsStyle.fontSizePx)
        assertEquals(10, fsStyle.positionPercent)
        assertEquals(0, fsStyle.backgroundOpacityPercent)
        assertEquals(0, fsStyle.lineSpacingPx)
        assertEquals(0, fsStyle.backgroundPaddingPx)
    }

    @Test
    fun saveAndGetVideoZoom_persistsCorrectly() {
        preferences.saveVideoZoom("125%")
        assertEquals("125%", preferences.getVideoZoom())
    }

    @Test
    fun saveAndGetVideoZoom_portraitAndFullscreen_persistSeparately() {
        preferences.saveVideoZoom("110%", isFullscreen = false)
        preferences.saveVideoZoom("135%", isFullscreen = true)

        assertEquals("110%", preferences.getVideoZoom(isFullscreen = false))
        assertEquals("135%", preferences.getVideoZoom(isFullscreen = true))
    }

    @Test
    fun saveAndGetPlaybackSpeed_persistsCorrectly() {
        preferences.savePlaybackSpeed(1.5f)
        assertEquals(1.5f, preferences.getPlaybackSpeed(), 0.01f)
    }

    @Test
    fun saveAndGetAutoNext_persistsCorrectly() {
        preferences.saveAutoNext(true)
        assertTrue(preferences.isAutoNext())
    }

    @Test
    fun saveAndGetQualityAndSubtitleId_persistsCorrectly() {
        preferences.savePreferredQuality("1080p")
        preferences.savePreferredSubtitleId("id")
        assertEquals("1080p", preferences.getPreferredQuality())
        assertEquals("id", preferences.getPreferredSubtitleId())
    }

    @Test
    fun saveAndGetSubtitleStyle_persistsCorrectly() {
        val portraitCustom = SubtitleStyleConfig(
            fontSizePx = 18,
            positionPercent = 8,
            backgroundOpacityPercent = 40,
            textColor = 0xFFFFFFFFL,
            baseBackgroundColor = 0xFF000000L,
            fontFamily = "Comic Sans MS",
            outlineStyle = "Thin"
        )
        val fullscreenCustom = SubtitleStyleConfig(
            fontSizePx = 36,
            positionPercent = 18,
            backgroundOpacityPercent = 75,
            textColor = 0xFFFFCC00L,
            baseBackgroundColor = 0xFF112233L,
            fontFamily = "Comic Sans MS",
            outlineStyle = "Thick"
        )

        preferences.saveSubtitleStyle(portraitCustom, isFullscreen = false)
        preferences.saveSubtitleStyle(fullscreenCustom, isFullscreen = true)

        val restoredPortrait = preferences.getSubtitleStyle(isFullscreen = false)
        assertEquals(18, restoredPortrait.fontSizePx)
        assertEquals(8, restoredPortrait.positionPercent)
        assertEquals(40, restoredPortrait.backgroundOpacityPercent)

        val restoredFullscreen = preferences.getSubtitleStyle(isFullscreen = true)
        assertEquals(36, restoredFullscreen.fontSizePx)
        assertEquals(18, restoredFullscreen.positionPercent)
        assertEquals(75, restoredFullscreen.backgroundOpacityPercent)
        assertEquals(0xFFFFCC00L, restoredFullscreen.textColor)
        assertEquals(0xFF112233L, restoredFullscreen.baseBackgroundColor)
        assertEquals("Comic Sans MS", restoredFullscreen.fontFamily)
        assertEquals("Thick", restoredFullscreen.outlineStyle)
    }
}
