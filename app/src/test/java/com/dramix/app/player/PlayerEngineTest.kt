package com.dramix.app.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dramix.app.player.controller.DramixPlayerController
import com.dramix.app.player.engine.CacheManager
import com.dramix.app.player.engine.HeaderInjectingDataSourceFactory
import com.dramix.app.player.engine.PlayerFactory
import com.dramix.app.player.model.PlaybackState
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerEngineTest {

    private lateinit var context: Context
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var headerDataSourceFactory: HeaderInjectingDataSourceFactory

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        okHttpClient = OkHttpClient.Builder().build()
        headerDataSourceFactory = HeaderInjectingDataSourceFactory(okHttpClient)
    }

    @After
    fun tearDown() {
        CacheManager.releaseCache()
    }

    @Test
    fun headerInjectingDataSourceFactory_manages_headers() {
        val headers = mapOf(
            "Referer" to "https://wetv.vip/",
            "User-Agent" to "Custom/1.0"
        )
        headerDataSourceFactory.setHeaders(headers)

        val retrieved = headerDataSourceFactory.getHeaders()
        assertEquals("https://wetv.vip/", retrieved["Referer"])
        assertEquals("Custom/1.0", retrieved["User-Agent"])

        headerDataSourceFactory.clearHeaders()
        assertTrue(headerDataSourceFactory.getHeaders().isEmpty())
    }

    @Test
    fun cacheManager_creates_and_releases_simple_cache() {
        val cache = CacheManager.getCache(context)
        assertNotNull(cache)
        CacheManager.releaseCache()
    }

    @Test
    fun playerFactory_creates_exoplayer_instance() {
        val playerFactory = PlayerFactory(context, okHttpClient)
        val (player, headerFactory) = playerFactory.createPlayer(enableCache = false)
        assertNotNull(player)
        assertNotNull(headerFactory)
        player.release()
    }

    @Test
    fun dramixPlayerController_prepares_and_updates_headers() {
        val playerFactory = PlayerFactory(context, okHttpClient)
        val (player, headerFactory) = playerFactory.createPlayer(enableCache = false)
        val controller = DramixPlayerController(player, headerFactory)

        assertEquals(PlaybackState.Idle, controller.playbackState.value)

        val headers = mapOf("Referer" to "https://freereels.cc/")
        controller.prepare(
            streamUrl = "https://cdn.example.com/video.mp4",
            headers = headers,
            startPositionMs = 5000L,
            autoPlay = false
        )

        assertEquals("https://freereels.cc/", headerFactory.getHeaders()["Referer"])
        controller.release()
    }
}
