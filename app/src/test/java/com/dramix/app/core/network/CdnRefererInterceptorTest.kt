package com.dramix.app.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CdnRefererInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun interceptor_adds_moviebox_referer_for_hakunaymatata_host() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val interceptor = CdnRefererInterceptor()
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        // Create request matching hakunaymatata host
        val request = Request.Builder()
            .url(server.url("/stream.mp4").newBuilder().host("bcdnxw.hakunaymatata.com").build())
            .build()

        // Test interceptor directly
        var capturedReferer: String? = null
        val testClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                capturedReferer = chain.request().header("Referer")
                chain.proceed(chain.request().newBuilder().url(server.url("/stream.mp4")).build())
            }
            .build()

        testClient.newCall(request).execute().close()

        assertEquals("https://moviebox.ph/", capturedReferer)
    }

    @Test
    fun interceptor_overrides_dramix_app_referer_for_moviebox_cdn() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val interceptor = CdnRefererInterceptor()
        var capturedReferer: String? = null
        val testClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                capturedReferer = chain.request().header("Referer")
                chain.proceed(chain.request().newBuilder().url(server.url("/stream.mp4")).build())
            }
            .build()

        val request = Request.Builder()
            .url(server.url("/stream.mp4").newBuilder().host("bcdnxw.hakunaymatata.com").build())
            .header("Referer", "https://dramix.app/")
            .build()

        testClient.newCall(request).execute().close()

        assertEquals("https://moviebox.ph/", capturedReferer)
    }

    @Test
    fun interceptor_does_not_modify_other_hosts() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val interceptor = CdnRefererInterceptor()
        var capturedReferer: String? = null
        val testClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                capturedReferer = chain.request().header("Referer")
                chain.proceed(chain.request())
            }
            .build()

        val request = Request.Builder()
            .url(server.url("/api/status"))
            .build()

        testClient.newCall(request).execute().close()

        assertNull(capturedReferer)
    }
}
