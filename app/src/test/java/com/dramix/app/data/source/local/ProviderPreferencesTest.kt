package com.dramix.app.data.source.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dramix.app.domain.model.ProviderModel
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
class ProviderPreferencesTest {

    private lateinit var context: Context
    private lateinit var preferences: ProviderPreferences

    private val sampleRawProviders = listOf(
        ProviderModel(id = "wetv", name = "WeTV", contentType = "long_drama"),
        ProviderModel(id = "viu", name = "VIU", contentType = "long_drama"),
        ProviderModel(id = "freereels", name = "FreeReels", contentType = "short_drama"),
        ProviderModel(id = "kisskh", name = "KissKH", contentType = "long_drama")
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        preferences = ProviderPreferences(context)
        preferences.resetToDefault()
    }

    @Test
    fun applyToProviders_returnsRawList_whenNoSavedConfigs() {
        val result = preferences.applyToProviders(sampleRawProviders)
        assertEquals(sampleRawProviders.size, result.size)
        assertEquals("wetv", result[0].id)
    }

    @Test
    fun saveConfigs_persistsAndReordersProviders() {
        val customConfigs = listOf(
            UserProviderConfig(id = "freereels", isEnabled = true),
            UserProviderConfig(id = "kisskh", isEnabled = true),
            UserProviderConfig(id = "wetv", isEnabled = false),
            UserProviderConfig(id = "viu", isEnabled = true)
        )

        preferences.saveConfigs(customConfigs)

        val result = preferences.applyToProviders(sampleRawProviders)
        // wetv is disabled, so remaining: freereels, kisskh, viu
        assertEquals(3, result.size)
        assertEquals("freereels", result[0].id)
        assertEquals("kisskh", result[1].id)
        assertEquals("viu", result[2].id)
        assertFalse(result.any { it.id == "wetv" })
    }

    @Test
    fun applyToProviders_appendsNewGatewayProvidersAtEnd() {
        val customConfigs = listOf(
            UserProviderConfig(id = "freereels", isEnabled = true)
        )
        preferences.saveConfigs(customConfigs)

        val result = preferences.applyToProviders(sampleRawProviders)
        assertEquals(4, result.size)
        assertEquals("freereels", result[0].id)
        // wetv, viu, kisskh should be appended
        assertTrue(result.any { it.id == "wetv" })
        assertTrue(result.any { it.id == "viu" })
        assertTrue(result.any { it.id == "kisskh" })
    }

    @Test
    fun resetToDefault_clearsCustomizations() {
        val customConfigs = listOf(
            UserProviderConfig(id = "freereels", isEnabled = true),
            UserProviderConfig(id = "wetv", isEnabled = false)
        )
        preferences.saveConfigs(customConfigs)

        preferences.resetToDefault()

        val saved = preferences.getSavedConfigs()
        assertTrue(saved.isEmpty())

        val result = preferences.applyToProviders(sampleRawProviders)
        assertEquals(sampleRawProviders.size, result.size)
        assertEquals("wetv", result[0].id)
    }
}
