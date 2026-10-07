package com.dramix.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "download_record",
    indices = [
        Index(value = ["media_id"], unique = true),
        Index(value = ["status", "created_at"]),
        Index(value = ["drama_id", "provider_id"])
    ]
)
data class DownloadRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "media_id")
    val mediaId: String,

    @ColumnInfo(name = "drama_id")
    val dramaId: String,

    @ColumnInfo(name = "provider_id")
    val providerId: String,

    @ColumnInfo(name = "drama_title")
    val dramaTitle: String,

    @ColumnInfo(name = "episode_number")
    val episodeNumber: Int,

    @ColumnInfo(name = "episode_title")
    val episodeTitle: String? = null,

    @ColumnInfo(name = "stream_url")
    val streamUrl: String,

    @ColumnInfo(name = "local_uri")
    val localUri: String? = null,

    @ColumnInfo(name = "bytes_downloaded")
    val bytesDownloaded: Long = 0L,

    @ColumnInfo(name = "total_bytes")
    val totalBytes: Long = 0L,

    @ColumnInfo(name = "progress_percentage")
    val progressPercentage: Int = 0,

    @ColumnInfo(name = "status")
    val status: String = "QUEUED", // QUEUED, DOWNLOADING, COMPLETED, FAILED, PAUSED

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
)
