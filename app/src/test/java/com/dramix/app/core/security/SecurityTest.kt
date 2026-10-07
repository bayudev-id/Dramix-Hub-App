package com.dramix.app.core.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dramix.app.core.network.SecurityHeadersInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecurityTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var deviceIdentifier: DeviceIdentifier
    private lateinit var securityManager: SecurityManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        deviceIdentifier = DeviceIdentifier(context)
        securityManager = SecurityManager()
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun deviceIdentifier_produces_consistent_sha256_hash() {
        val id1 = deviceIdentifier.getDeviceId()
        val id2 = deviceIdentifier.getDeviceId()

        assertNotNull(id1)
        assertEquals(64, id1.length) // SHA-256 hex string is 64 characters
        assertEquals(id1, id2) // Cached fingerprint must be identical
        assertTrue(id1.matches(Regex("^[a-f0-9]{64}$")))
    }

    @Test
    fun securityManager_evaluates_environment_without_crash() {
        val threats = securityManager.getDetectedThreats()
        assertNotNull(threats)
    }

    @Test
    fun securityHeadersInterceptor_injects_required_headers() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val client = OkHttpClient.Builder()
            .addInterceptor(SecurityHeadersInterceptor(deviceIdentifier))
            .build()

        val request = Request.Builder()
            .url(mockWebServer.url("/v1/models"))
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }

        val recordedRequest = mockWebServer.takeRequest()
        val deviceIdHeader = recordedRequest.getHeader("X-Device-Id")
        val timestampHeader = recordedRequest.getHeader("X-Timestamp")
        val userAgentHeader = recordedRequest.getHeader("User-Agent")

        assertEquals(deviceIdentifier.getDeviceId(), deviceIdHeader)
        assertNotNull(timestampHeader)
        assertTrue(timestampHeader!!.toLong() > 0)
        assertEquals("Dramix/1.0.0 (Android; Client)", userAgentHeader)
    }
}
