package com.dramix.app.player.engine

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
class HeaderInjectingDataSourceFactory(
    private val okHttpClient: OkHttpClient,
    private val chunkSizeBytes: Long = 10L * 1024 * 1024 // 10 MB default
) : DataSource.Factory {

    private val dynamicHeaders = ConcurrentHashMap<String, String>()
    private var streamUrl: String = ""
    private var streamFormat: String = ""

    fun setHeaders(headers: Map<String, String>) {
        dynamicHeaders.clear()
        dynamicHeaders.putAll(headers)
    }

    fun getOkHttpClient(): OkHttpClient = okHttpClient

    fun addHeader(key: String, value: String) {
        dynamicHeaders[key] = value
    }

    fun getHeaders(): Map<String, String> = dynamicHeaders.toMap()

    fun clearHeaders() {
        dynamicHeaders.clear()
    }

    fun setStreamMetadata(url: String, format: String) {
        streamUrl = url
        streamFormat = format.lowercase()
    }

    override fun createDataSource(): DataSource {
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        if (dynamicHeaders.isNotEmpty()) {
            okHttpDataSourceFactory.setDefaultRequestProperties(dynamicHeaders)
        }
        val httpDataSource = okHttpDataSourceFactory.createDataSource()
        
        // Only wrap with BoundedRangeDataSource for progressive formats (MP4)
        // Skip for streaming protocols (HLS, DASH, etc)
        val shouldApplyBounding = when {
            streamFormat == "mp4" -> true
            streamUrl.contains(".mp4", ignoreCase = true) -> true
            streamUrl.contains("hakunaymatata.com") -> true  // MovieBox CDN
            streamFormat.contains("hls") || streamUrl.contains(".m3u8") -> false
            streamFormat.contains("dash") || streamUrl.contains(".mpd") -> false
            else -> false
        }
        
        return if (shouldApplyBounding) {
            BoundedRangeDataSource(httpDataSource, chunkSizeBytes)
        } else {
            httpDataSource
        }
    }
}
