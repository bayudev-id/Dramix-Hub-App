package com.dramix.app.core.network

import androidx.media3.common.PlaybackException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
class AppErrorSanitizerTest {

    @Test
    fun sanitize_strips_raw_urls_and_ports_and_domains() {
        val raw = "Terjadi kesalahan stream on http://127.0.0.1:6105/vod/vuclip_vod.m3u8?api_key=912ursfh283fjefw"
        val cleaned = AppErrorSanitizer.sanitize(raw)

        assertFalse(cleaned.contains("http://"))
        assertFalse(cleaned.contains("127.0.0.1"))
        assertFalse(cleaned.contains("6105"))
        assertFalse(cleaned.contains("api_key"))
        assertFalse(cleaned.contains("912ursfh283fjefw"))
    }

    @Test
    fun sanitize_strips_https_cloudflare_domain_and_endpoints() {
        val raw = "Gagal menghubungi https://api.dramix.web.id/api/modelles/source?model_id=viu&token=pb_master_token_2026"
        val cleaned = AppErrorSanitizer.sanitize(raw)

        assertFalse(cleaned.contains("https://"))
        assertFalse(cleaned.contains("api.dramix.web.id"))
        assertFalse(cleaned.contains("/api/modelles/source"))
        assertFalse(cleaned.contains("token"))
        assertFalse(cleaned.contains("pb_master_token_2026"))
    }

    @Test
    fun format_maps_network_exceptions_to_err_net_codes() {
        val unknownHost = UnknownHostException("Unable to resolve host api.dramix.web.id: No address associated")
        val formattedHost = AppErrorSanitizer.format(unknownHost)
        assertTrue(formattedHost.startsWith("[ERR_NET_002]"))
        assertFalse(formattedHost.contains("api.dramix.web.id"))

        val timeout = SocketTimeoutException("timeout on https://api.dramix.web.id")
        val formattedTimeout = AppErrorSanitizer.format(timeout)
        assertTrue(formattedTimeout.startsWith("[ERR_NET_001]"))
        assertFalse(formattedTimeout.contains("api.dramix.web.id"))

        val connect = ConnectException("Failed to connect to /127.0.0.1:8090")
        val formattedConnect = AppErrorSanitizer.format(connect)
        assertTrue(formattedConnect.startsWith("[ERR_NET_003]"))
        assertFalse(formattedConnect.contains("127.0.0.1:8090"))
    }

    @Test
    fun format_maps_http_exceptions_to_err_srv_codes() {
        val body = "{}".toResponseBody("application/json".toMediaType())
        val http502 = HttpException(Response.error<String>(502, body))
        val formatted502 = AppErrorSanitizer.format(http502)
        assertTrue(formatted502.startsWith("[ERR_SRV_502]"))

        val http404 = HttpException(Response.error<String>(404, body))
        val formatted404 = AppErrorSanitizer.format(http404)
        assertTrue(formatted404.startsWith("[ERR_SRV_404]"))
    }

    @Test
    fun format_maps_playback_exceptions_to_err_ply_codes() {
        val ex = PlaybackException(
            "Playback error on http://127.0.0.1:6105/vod/test.m3u8",
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        )
        val formatted = AppErrorSanitizer.formatPlayback(ex)
        assertTrue(formatted.startsWith("[ERR_PLY_002]"))
        assertFalse(formatted.contains("http://"))
        assertFalse(formatted.contains("127.0.0.1"))
    }

    @Test
    fun format_maps_license_keywords_correctly() {
        val exAlreadyBound = RuntimeException("Lisensi sudah terikat ke HP lain")
        val formattedBound = AppErrorSanitizer.formatLicense(exAlreadyBound)
        assertTrue(formattedBound.startsWith("[ERR_LIC_003]"))

        val exExpired = RuntimeException("Masa aktif lisensi telah berakhir")
        val formattedExpired = AppErrorSanitizer.formatLicense(exExpired)
        assertTrue(formattedExpired.startsWith("[ERR_LIC_002]"))
    }
}
