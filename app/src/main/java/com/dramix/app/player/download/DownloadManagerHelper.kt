package com.dramix.app.player.download

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.dramix.app.player.engine.HeaderInjectingDataSourceFactory
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
object DownloadManagerHelper {

    const val CHANNEL_ID = "dramix_downloads"
    const val NOTIFICATION_ID = 1001

    private const val DOWNLOAD_DIR = "dramix_downloads"

    @Volatile
    private var downloadCache: Cache? = null

    @Volatile
    private var databaseProvider: DatabaseProvider? = null

    @Volatile
    private var downloadManager: DownloadManager? = null

    @Volatile
    private var notificationHelper: DownloadNotificationHelper? = null

    @Volatile
    private var headerDataSourceFactory: HeaderInjectingDataSourceFactory? = null

    @Synchronized
    fun getDatabaseProvider(context: Context): DatabaseProvider {
        return databaseProvider ?: StandaloneDatabaseProvider(context.applicationContext).also {
            databaseProvider = it
        }
    }

    @Synchronized
    fun getDownloadCache(context: Context): Cache {
        return downloadCache ?: run {
            val downloadDir = File(context.applicationContext.filesDir, DOWNLOAD_DIR)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            SimpleCache(downloadDir, NoOpCacheEvictor(), getDatabaseProvider(context)).also {
                downloadCache = it
            }
        }
    }

    @Synchronized
    fun getHttpDataSourceFactory(okHttpClient: OkHttpClient): HeaderInjectingDataSourceFactory {
        return headerDataSourceFactory ?: run {
            val factory = HeaderInjectingDataSourceFactory(okHttpClient)
            // Inject valid default User-Agent and Referer headers for CDN HLS segment fetches
            factory.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 Dramix/1.0")
            factory.addHeader("Referer", "https://dramix.app/")
            headerDataSourceFactory = factory
            factory
        }
    }

    @Synchronized
    fun getDownloadManager(context: Context, okHttpClient: OkHttpClient): DownloadManager {
        return downloadManager ?: run {
            val executor = Executors.newFixedThreadPool(3)
            DownloadManager(
                context.applicationContext,
                getDatabaseProvider(context),
                getDownloadCache(context),
                getHttpDataSourceFactory(okHttpClient),
                executor
            ).apply {
                maxParallelDownloads = 2
            }.also {
                downloadManager = it
            }
        }
    }

    @Synchronized
    fun getNotificationHelper(context: Context): DownloadNotificationHelper {
        return notificationHelper ?: DownloadNotificationHelper(context.applicationContext, CHANNEL_ID).also {
            notificationHelper = it
        }
    }

    @Synchronized
    fun release() {
        downloadManager?.release()
        downloadManager = null
        downloadCache?.release()
        downloadCache = null
        databaseProvider = null
        headerDataSourceFactory = null
    }
}
