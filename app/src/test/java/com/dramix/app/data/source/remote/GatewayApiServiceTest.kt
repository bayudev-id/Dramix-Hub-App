package com.dramix.app.data.source.remote

import com.dramix.app.core.network.RetrofitProvider
import com.dramix.app.data.repository.CatalogRepositoryImpl
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GatewayApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: GatewayApiService
    private lateinit var repository: CatalogRepositoryImpl

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val okHttpClient = OkHttpClient.Builder().build()
        apiService = RetrofitProvider.createGatewayService(
            okHttpClient = okHttpClient,
            baseUrl = mockWebServer.url("/").toString()
        )
        repository = CatalogRepositoryImpl(apiService)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun getModels_returns_provider_list() = runTest {
        val json = """
        {
            "code": 200,
            "message": "success",
            "data": [
                {
                    "id": "wetv",
                    "name": "WeTV",
                    "icon_url": "https://example.com/wetv.png",
                    "description": "Asian dramas",
                    "content_type": "long_drama",
                    "status": "active"
                },
                {
                    "id": "freereels",
                    "name": "FreeReels",
                    "icon_url": "https://example.com/freereels.png",
                    "description": "Short dramas",
                    "content_type": "short_drama",
                    "status": "active"
                }
            ]
        }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getProviders()
        assertTrue(result.isSuccess)
        val providers = result.getOrThrow()
        assertEquals(2, providers.size)
        assertEquals("wetv", providers[0].id)
        assertEquals("WeTV", providers[0].name)
        assertEquals("long_drama", providers[0].contentType)
        assertEquals("freereels", providers[1].id)
        assertEquals("short_drama", providers[1].contentType)
    }

    @Test
    fun getCategories_returns_category_list() = runTest {
        val json = """
        {
            "code": 200,
            "message": "success",
            "data": [
                { "id": "trending", "name": "Trending" },
                { "id": "romance", "name": "Romance" }
            ]
        }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getCategories("wetv")
        assertTrue(result.isSuccess)
        val categories = result.getOrThrow()
        assertEquals(2, categories.size)
        assertEquals("trending", categories[0].id)
        assertEquals("Trending", categories[0].name)
    }

    @Test
    fun getVideos_handles_empty_items_safely() = runTest {
        val json = """
        {
            "code": 200,
            "message": "success",
            "data": {
                "model_id": "dramaboxbaru",
                "category_id": "all",
                "page": 1,
                "items": []
            }
        }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getVideos("dramaboxbaru", "all", 1)
        assertTrue(result.isSuccess)
        val items = result.getOrThrow()
        assertTrue(items.isEmpty())
    }

    @Test
    fun getDetail_maps_seasons_and_episodes_correctly() = runTest {
        val json = """
        {
            "code": 200,
            "message": "success",
            "data": {
                "id": "drama-99",
                "title": "Hidden Love",
                "cover": "https://example.com/cover.jpg",
                "description": "School romance drama",
                "type": "long_drama",
                "source": "WeTV",
                "release_date": "2023-06-20",
                "score": "9.2",
                "views": "15.4M",
                "is_vip": false,
                "tags": ["Romance", "Youth"],
                "total_episodes": 25,
                "seasons": [
                    {
                        "name": "Season 1",
                        "index": 1,
                        "total_episodes": 25,
                        "episodes": [
                            {
                                "id": "ep-1",
                                "title": "Episode 1",
                                "number": 1,
                                "cover": "https://example.com/ep1.jpg",
                                "duration_seconds": 2700,
                                "is_vip": false,
                                "is_express": false,
                                "is_trailer": false,
                                "label": "",
                                "tags": []
                            },
                            {
                                "id": "ep-4",
                                "title": "Episode 4",
                                "number": 4,
                                "cover": "https://example.com/ep4.jpg",
                                "duration_seconds": 2700,
                                "is_vip": true,
                                "is_express": false,
                                "is_trailer": false,
                                "label": "VIP",
                                "tags": ["VIP"]
                            }
                        ]
                    }
                ]
            }
        }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getDramaDetail("wetv", "drama-99")
        assertTrue(result.isSuccess)
        val detail = result.getOrThrow()
        assertEquals("Hidden Love", detail.title)
        assertEquals(1, detail.seasons.size)
        assertEquals(2, detail.seasons[0].episodes.size)
        assertEquals(false, detail.seasons[0].episodes[0].isVip)
        assertEquals(true, detail.seasons[0].episodes[1].isVip)
    }

    @Test
    fun getSource_extracts_stream_url_and_headers() = runTest {
        val json = """
        {
            "code": 200,
            "message": "success",
            "data": {
                "id": "drama-99",
                "episode_id": "ep-1",
                "duration_seconds": 2698,
                "streams": [
                    {
                        "quality": "1080p",
                        "format": "m3u8",
                        "url": "https://cdn.example.com/stream.m3u8",
                        "is_drm": false,
                        "headers": {
                            "Referer": "https://wetv.vip/",
                            "User-Agent": "Mozilla/5.0"
                        }
                    }
                ],
                "subtitles": [
                    {
                        "lang": "id",
                        "label": "Bahasa Indonesia",
                        "url": "https://cdn.example.com/sub_id.vtt"
                    }
                ]
            }
        }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getPlaybackSource("wetv", "ep-1", "drama-99")
        assertTrue(result.isSuccess)
        val source = result.getOrThrow()
        assertEquals(1, source.streams.size)
        assertEquals("https://cdn.example.com/stream.m3u8", source.streams[0].url)
        assertEquals("https://wetv.vip/", source.streams[0].headers["Referer"])
        assertEquals(1, source.subtitles.size)
        assertEquals("id", source.subtitles[0].lang)
    }

    @Test
    fun search_returns_matched_items() = runTest {
        val json = """
        {
            "code": 200,
            "message": "success",
            "data": {
                "model_id": "moviebox",
                "q": "money",
                "page": 1,
                "items": [
                    {
                        "id": "money-heist-1",
                        "title": "Money Heist",
                        "cover": "https://example.com/poster.jpg",
                        "type": "drama",
                        "source": "MovieBox",
                        "episode_info": "2017-05-02",
                        "score": "8.2",
                        "views": "",
                        "is_vip": false,
                        "tags": ["Action", "Crime"]
                    }
                ]
            }
        }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.search("moviebox", "money", 1)
        assertTrue(result.isSuccess)
        val items = result.getOrThrow()
        assertEquals(1, items.size)
        assertEquals("Money Heist", items[0].title)
    }
}
