package com.dramix.app.player.download

import android.app.Notification
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.dramix.app.R
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
class DramixDownloadService : DownloadService(
    DownloadManagerHelper.NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DownloadManagerHelper.CHANNEL_ID,
    R.string.download_channel_name,
    R.string.download_channel_description
) {

    override fun getDownloadManager(): DownloadManager {
        return DownloadManagerHelper.getDownloadManager(this, OkHttpClient())
    }

    override fun getScheduler(): Scheduler? {
        return null
    }

    override fun getForegroundNotification(
        downloads: List<Download>,
        notMetRequirements: Int
    ): Notification {
        return DownloadManagerHelper.getNotificationHelper(this)
            .buildProgressNotification(
                this,
                R.mipmap.ic_launcher,
                null,
                null,
                downloads,
                notMetRequirements
            )
    }
}
