package com.dramix.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmark",
    indices = [
        Index(value = ["drama_id", "provider_id"], unique = true),
        Index(value = ["created_at"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "drama_id")
    val dramaId: String,

    @ColumnInfo(name = "provider_id")
    val providerId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "poster_url")
    val posterUrl: String? = null,

    @ColumnInfo(name = "content_type")
    val contentType: String = "long_drama",

    @ColumnInfo(name = "rating")
    val rating: String? = null,

    @ColumnInfo(name = "total_episodes")
    val totalEpisodes: Int? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
