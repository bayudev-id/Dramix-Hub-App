package com.dramix.app.data.source.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GatewayResponse<T>(
    @Json(name = "code") val code: Int = 200,
    @Json(name = "message") val message: String = "success",
    @Json(name = "data") val data: T? = null
)

@JsonClass(generateAdapter = true)
data class ProviderModelDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "icon_url") val iconUrl: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "content_type") val contentType: String? = "long_drama",
    @Json(name = "status") val status: String? = "active"
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class CategoryFeedDataDto(
    @Json(name = "model_id") val modelId: String? = null,
    @Json(name = "data") val data: List<CategoryDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class VideoFeedDataDto(
    @Json(name = "model_id") val modelId: String? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "page") val page: Int? = 1,
    @Json(name = "items") val items: List<VideoItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class VideoItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "episode_info") val episodeInfo: String? = null,
    @Json(name = "score") val score: String? = null,
    @Json(name = "views") val views: String? = null,
    @Json(name = "is_vip") val isVip: Boolean = false,
    @Json(name = "tags") val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DramaDetailDataDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "score") val score: String? = null,
    @Json(name = "views") val views: String? = null,
    @Json(name = "is_vip") val isVip: Boolean = false,
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "total_episodes") val totalEpisodes: Int = 0,
    @Json(name = "seasons") val seasons: List<SeasonDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SeasonDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "index") val index: Int = 1,
    @Json(name = "total_episodes") val totalEpisodes: Int = 0,
    @Json(name = "episodes") val episodes: List<EpisodeDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class EpisodeDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "number") val number: Int = 1,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "duration_seconds") val durationSeconds: Int? = null,
    @Json(name = "is_vip") val isVip: Boolean = false,
    @Json(name = "is_express") val isExpress: Boolean = false,
    @Json(name = "is_trailer") val isTrailer: Boolean = false,
    @Json(name = "label") val label: String? = null,
    @Json(name = "tags") val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SearchRequestDto(
    @Json(name = "model_id") val modelId: String,
    @Json(name = "q") val q: String,
    @Json(name = "page") val page: Int = 1,
    @Json(name = "content_type") val contentType: String? = null
)

@JsonClass(generateAdapter = true)
data class SearchDataDto(
    @Json(name = "model_id") val modelId: String? = null,
    @Json(name = "q") val q: String? = null,
    @Json(name = "page") val page: Int? = 1,
    @Json(name = "items") val items: List<VideoItemDto> = emptyList()
)
