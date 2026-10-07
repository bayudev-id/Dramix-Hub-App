package com.dramix.app.player.engine

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@OptIn(UnstableApi::class)
object CacheManager {

    private const val CACHE_DIR_NAME = "dramix_media_cache"
    private const val MAX_CACHE_BYTES = 200L * 1024 * 1024 // 200 MB

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: run {
                val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
                val databaseProvider = StandaloneDatabaseProvider(context)
                SimpleCache(cacheDir, evictor, databaseProvider).also {
                    simpleCache = it
                }
            }
        }
    }

    @Synchronized
    fun releaseCache() {
        simpleCache?.release()
        simpleCache = null
    }

    fun getCacheSizeBytes(context: Context): Long {
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!cacheDir.exists()) return 0L
        return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    @Synchronized
    fun clearCache(context: Context) {
        releaseCache()
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
        }
    }
}
