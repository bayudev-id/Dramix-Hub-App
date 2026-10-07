package com.dramix.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_history",
    indices = [
        Index(value = ["drama_id", "provider_id", "episode_number"], unique = true),
        Index(value = ["updated_at"]),
        Index(value = ["drama_id", "provider_id", "updated_at"])
    ]
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "drama_id")
    val dramaId: String,

    @ColumnInfo(name = "provider_id")
    val providerId: String,

    @ColumnInfo(name = "drama_title")
    val dramaTitle: String,

    @ColumnInfo(name = "drama_poster")
    val dramaPoster: String? = null,

    @ColumnInfo(name = "episode_number")
    val episodeNumber: Int,

    @ColumnInfo(name = "episode_title")
    val episodeTitle: String? = null,

    @ColumnInfo(name = "position_ms")
    val positionMs: Long = 0L,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0L,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
