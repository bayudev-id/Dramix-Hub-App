package com.dramix.app.player.drm

import android.util.Base64
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.drm.ExoMediaDrm
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback
import androidx.media3.exoplayer.drm.MediaDrmCallback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

@OptIn(UnstableApi::class)
class WidevineDrmCallback(
    private val defaultLicenseUrl: String?,
    private val licenseParams: Map<String, String>,
    private val headers: Map<String, String>,
    private val okHttpClient: OkHttpClient
) : MediaDrmCallback {

    companion object {
        private const val TAG = "WidevineDrmCallback"
    }

    private val defaultDrmCallback = HttpMediaDrmCallback(
        defaultLicenseUrl,
        DefaultHttpDataSource.Factory()
    )

    override fun executeProvisionRequest(
        uuid: UUID,
        request: ExoMediaDrm.ProvisionRequest
    ): ByteArray {
        Log.d(TAG, "Delegating executeProvisionRequest to default HttpMediaDrmCallback: url=${request.defaultUrl}")
        return defaultDrmCallback.executeProvisionRequest(uuid, request)
    }

    override fun executeKeyRequest(
        uuid: UUID,
        request: ExoMediaDrm.KeyRequest
    ): ByteArray {
        val rawTargetUrl = request.licenseServerUrl.takeIf { !it.isNullOrBlank() }
            ?: defaultLicenseUrl
            ?: "https://drm-license.youku.tv/ups/drm.json"
        val targetUrl = rawTargetUrl.substringBefore("?")

        val challengeBase64 = Base64.encodeToString(request.data, Base64.NO_WRAP)

        val formBodyBuilder = FormBody.Builder()
        var hasDrmType = false
        for ((key, value) in licenseParams) {
            if (key.equals("drmType", ignoreCase = true)) {
                formBodyBuilder.add("drmType", "widevine")
                hasDrmType = true
            } else if (!key.equals("licenseRequest", ignoreCase = true)) {
                formBodyBuilder.add(key, value)
            }
        }
        if (!hasDrmType) {
            formBodyBuilder.add("drmType", "widevine")
        }
        formBodyBuilder.add("licenseRequest", challengeBase64)
        val formBody = formBodyBuilder.build()

        val reqBuilder = Request.Builder()
            .url(targetUrl)
            .post(formBody)
            .header("User-Agent", headers["User-Agent"] ?: "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36")

        for ((k, v) in headers) {
            if (!k.equals("Content-Type", ignoreCase = true) && !k.equals("User-Agent", ignoreCase = true)) {
                reqBuilder.header(k, v)
            }
        }

        val okHttpRequest = reqBuilder.build()
        Log.d(TAG, "Requesting DRM key from $targetUrl with params count ${licenseParams.size}")

        okHttpClient.newCall(okHttpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.e(TAG, "DRM key request HTTP ${response.code}: $errBody")
                throw IOException("DRM key request failed HTTP ${response.code}: $errBody")
            }
            val respString = response.body?.string() ?: ""
            Log.d(TAG, "DRM key response status ${response.code}, body: $respString")

            if (respString.trim().startsWith("{")) {
                val json = JSONObject(respString)
                val states = json.optInt("states", -1)
                val dataB64 = json.optString("data", "")
                if (dataB64.isNotBlank() && (states == 1 || states == 0 || states == -1)) {
                    return Base64.decode(dataB64, Base64.DEFAULT)
                } else {
                    val msg = json.optString("msg", "states=$states")
                    throw IOException("DRM license error: $msg ($states)")
                }
            } else {
                return respString.toByteArray(Charsets.ISO_8859_1)
            }
        }
    }
}
