package com.dramix.app.ui.screens.player_tv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dramix.app.domain.model.Category
import com.dramix.app.domain.model.DramaDetail
import com.dramix.app.domain.model.PlaybackSource
import com.dramix.app.domain.model.ProviderModel
import com.dramix.app.domain.model.StreamSource
import com.dramix.app.domain.model.VideoItem
import com.dramix.app.domain.repository.CatalogRepository
import com.dramix.app.player.engine.PlayerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class LiveTvPlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var playerFactory: PlayerFactory

    private val mockTvProviders = listOf(
        ProviderModel(id = "CineTv", name = "CineTv Live", contentType = "live_tv")
    )

    private val mockCategories = listOf(
        Category(id = "ID", name = "Nasional"),
        Category(id = "SP", name = "Olahraga")
    )

    private val mockIdChannels = listOf(
        VideoItem(id = "rcti", title = "RCTI", cover = "https://example.com/rcti.png", episodeInfo = "Ikatan Cinta"),
        VideoItem(id = "tvri", title = "TVRI", cover = "https://example.com/tvri.png", episodeInfo = "Berita Nasional")
    )

    private val mockSpChannels = listOf(
        VideoItem(id = "bein1", title = "BeIN Sports 1", cover = "https://example.com/bein1.png", episodeInfo = "La Liga Live")
    )

    private class FakeTvCatalogRepository(
        private val providers: List<ProviderModel>,
        private val categories: List<Category>,
        private val idChannels: List<VideoItem>,
        private val spChannels: List<VideoItem>,
        var shouldFailSource: Boolean = false
    ) : CatalogRepository {

        override suspend fun getProviders(): Result<List<ProviderModel>> = Result.success(providers)

        override suspend fun getCategories(modelId: String): Result<List<Category>> = Result.success(categories)

        override suspend fun getVideos(modelId: String, categoryId: String, page: Int): Result<List<VideoItem>> {
            return when (categoryId) {
                "SP" -> Result.success(spChannels)
                else -> Result.success(idChannels)
            }
        }

        override suspend fun getDramaDetail(modelId: String, id: String): Result<DramaDetail> = Result.failure(
            UnsupportedOperationException()
        )

        override suspend fun getPlaybackSource(modelId: String, episodeId: String, id: String?): Result<PlaybackSource> {
            if (shouldFailSource) {
                return Result.failure(RuntimeException("Sinyal satelit terputus"))
            }

            return Result.success(
                PlaybackSource(
                    id = id ?: episodeId,
                    episodeId = episodeId,
                    durationSeconds = 0, // Live stream duration is dynamic / zero
                    streams = listOf(
                        StreamSource(
                            quality = "1080p",
                            format = "m3u8",
                            url = "https://live.example.com/$episodeId/index.m3u8",
                            headers = mapOf("Referer" to "https://cinetv.live/")
                        )
                    )
                )
            )
        }

        override suspend fun search(modelId: String, query: String, page: Int, contentType: String?): Result<List<VideoItem>> = Result.success(emptyList())
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        playerFactory = PlayerFactory(context, OkHttpClient())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun liveTv_initializes_and_plays_first_channel() = runTest {
        val repo = FakeTvCatalogRepository(mockTvProviders, mockCategories, mockIdChannels, mockSpChannels)

        val viewModel = LiveTvPlayerViewModel(
            initialProviderId = "CineTv",
            initialChannelId = null,
            catalogRepository = repo,
            playerFactory = playerFactory,
            enablePlayerCache = false
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingFeed)
        assertEquals(2, state.categories.size)
        assertEquals("ID", state.selectedCategoryId)
        assertEquals(2, state.channels.size)
        assertEquals("RCTI", state.selectedChannel?.title)
        assertNotNull(state.playbackSource)
        assertEquals("https://live.example.com/rcti/index.m3u8", state.playbackSource?.streams?.firstOrNull()?.url)

        viewModel.release()
    }

    @Test
    fun liveTv_switches_channel_seamlessly() = runTest {
        val repo = FakeTvCatalogRepository(mockTvProviders, mockCategories, mockIdChannels, mockSpChannels)

        val viewModel = LiveTvPlayerViewModel(
            initialProviderId = "CineTv",
            initialChannelId = null,
            catalogRepository = repo,
            playerFactory = playerFactory,
            enablePlayerCache = false
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        // Switch to TVRI
        val tvri = mockIdChannels.find { it.id == "tvri" }!!
        viewModel.playChannel(tvri)

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals("tvri", state.selectedChannel?.id)
        assertEquals("TVRI", state.selectedChannel?.title)
        assertEquals("https://live.example.com/tvri/index.m3u8", state.playbackSource?.streams?.firstOrNull()?.url)

        viewModel.release()
    }

    @Test
    fun liveTv_filters_channels_by_category() = runTest {
        val repo = FakeTvCatalogRepository(mockTvProviders, mockCategories, mockIdChannels, mockSpChannels)

        val viewModel = LiveTvPlayerViewModel(
            initialProviderId = "CineTv",
            initialChannelId = null,
            catalogRepository = repo,
            playerFactory = playerFactory,
            enablePlayerCache = false
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        // Switch to Olahraga (SP) category
        val sportsCategory = mockCategories.find { it.id == "SP" }!!
        viewModel.selectCategory(sportsCategory)

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals("SP", state.selectedCategoryId)
        assertEquals(1, state.channels.size)
        assertEquals("BeIN Sports 1", state.channels.first().title)

        viewModel.release()
    }

    @Test
    fun liveTv_handles_stream_error_and_allows_retry() = runTest {
        val repo = FakeTvCatalogRepository(
            mockTvProviders,
            mockCategories,
            mockIdChannels,
            mockSpChannels,
            shouldFailSource = true
        )

        val viewModel = LiveTvPlayerViewModel(
            initialProviderId = "CineTv",
            initialChannelId = null,
            catalogRepository = repo,
            playerFactory = playerFactory,
            enablePlayerCache = false
        )

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isStreamError)
        assertEquals("Sinyal satelit terputus", state.errorMessage)

        // Fix source failure and retry
        repo.shouldFailSource = false
        viewModel.retryCurrentChannel()

        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()

        val updatedState = viewModel.uiState.value
        assertFalse(updatedState.isStreamError)
        assertNotNull(updatedState.playbackSource)
        assertEquals("https://live.example.com/rcti/index.m3u8", updatedState.playbackSource?.streams?.firstOrNull()?.url)

        viewModel.release()
    }
}
