package com.dramix.app.data.source.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlaybackSourceDataDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "episode_id") val episodeId: String? = null,
    @Json(name = "duration_seconds") val durationSeconds: Int? = null,
    @Json(name = "streams") val streams: List<StreamDto> = emptyList(),
    @Json(name = "subtitles") val subtitles: List<SubtitleDto> = emptyList(),
    @Json(name = "headers") val headers: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class StreamDto(
    @Json(name = "quality") val quality: String? = null,
    @Json(name = "format") val format: String? = null,
    @Json(name = "url") val url: String,
    @Json(name = "is_drm") val isDrm: Boolean = false,
    @Json(name = "headers") val headers: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class SubtitleDto(
    @Json(name = "lang") val lang: String? = null,
    @Json(name = "label") val label: String? = null,
    @Json(name = "url") val url: String
)
