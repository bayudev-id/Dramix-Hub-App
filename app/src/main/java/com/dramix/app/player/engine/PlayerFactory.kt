package com.dramix.app.player.engine

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
class PlayerFactory(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    fun createPlayer(
        headerDataSourceFactory: HeaderInjectingDataSourceFactory = HeaderInjectingDataSourceFactory(okHttpClient),
        enableCache: Boolean = true
    ): Pair<ExoPlayer, HeaderInjectingDataSourceFactory> {
        val upstreamFactory = headerDataSourceFactory

        val dataSourceFactory = if (enableCache) {
            val cache = CacheManager.getCache(context)
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        } else {
            upstreamFactory
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 1500,
                /* maxBufferMs = */ 5000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val exoPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()

        return Pair(exoPlayer, headerDataSourceFactory)
    }
}
