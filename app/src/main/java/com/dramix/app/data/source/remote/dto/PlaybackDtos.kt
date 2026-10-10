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
    @Json(name = "headers") val headers: Map<String, String>? = null,
    @Json(name = "countdown_url") val countdownUrl: String? = null,
    @Json(name = "is_countdown") val isCountdown: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DrmDto(
    @Json(name = "license_url") val licenseUrl: String? = null,
    @Json(name = "license_method") val licenseMethod: String? = null,
    @Json(name = "license_params") val licenseParams: Map<String, String>? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "systems") val systems: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class StreamDto(
    @Json(name = "quality") val quality: String? = null,
    @Json(name = "format") val format: String? = null,
    @Json(name = "url") val url: String,
    @Json(name = "is_drm") val isDrm: Boolean = false,
    @Json(name = "drm") val drm: DrmDto? = null,
    @Json(name = "headers") val headers: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class SubtitleDto(
    @Json(name = "lang") val lang: String? = null,
    @Json(name = "label") val label: String? = null,
    @Json(name = "url") val url: String
)
