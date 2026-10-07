package com.dramix.app.player.engine

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
class HeaderInjectingDataSourceFactory(
    private val okHttpClient: OkHttpClient
) : DataSource.Factory {

    private val dynamicHeaders = ConcurrentHashMap<String, String>()

    fun setHeaders(headers: Map<String, String>) {
        dynamicHeaders.clear()
        dynamicHeaders.putAll(headers)
    }

    fun addHeader(key: String, value: String) {
        dynamicHeaders[key] = value
    }

    fun getHeaders(): Map<String, String> = dynamicHeaders.toMap()

    fun clearHeaders() {
        dynamicHeaders.clear()
    }

    override fun createDataSource(): DataSource {
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        if (dynamicHeaders.isNotEmpty()) {
            okHttpDataSourceFactory.setDefaultRequestProperties(dynamicHeaders)
        }
        return okHttpDataSourceFactory.createDataSource()
    }
}
