package com.dramix.app.domain.model

data class ProviderModel(
    val id: String,
    val name: String,
    val iconUrl: String? = null,
    val description: String? = null,
    val contentType: String = "long_drama",
    val status: String = "active"
)

data class Category(
    val id: String,
    val name: String
)

data class VideoItem(
    val id: String,
    val title: String,
    val cover: String? = null,
    val type: String? = null,
    val source: String? = null,
    val episodeInfo: String? = null,
    val score: String? = null,
    val views: String? = null,
    val isVip: Boolean = false,
    val tags: List<String> = emptyList()
)

data class DramaDetail(
    val id: String,
    val title: String,
    val cover: String? = null,
    val description: String? = null,
    val type: String? = null,
    val source: String? = null,
    val releaseDate: String? = null,
    val score: String? = null,
    val views: String? = null,
    val isVip: Boolean = false,
    val tags: List<String> = emptyList(),
    val totalEpisodes: Int = 0,
    val seasons: List<Season> = emptyList()
)

data class Season(
    val name: String? = null,
    val index: Int = 1,
    val totalEpisodes: Int = 0,
    val episodes: List<Episode> = emptyList()
)

data class Episode(
    val id: String,
    val title: String? = null,
    val number: Int = 1,
    val cover: String? = null,
    val durationSeconds: Int? = null,
    val isVip: Boolean = false,
    val isExpress: Boolean = false,
    val isTrailer: Boolean = false,
    val label: String? = null,
    val tags: List<String> = emptyList()
)

data class PlaybackSource(
    val id: String,
    val episodeId: String,
    val durationSeconds: Int? = null,
    val streams: List<StreamSource> = emptyList(),
    val subtitles: List<Subtitle> = emptyList(),
    val headers: Map<String, String> = emptyMap()
)

data class StreamSource(
    val quality: String? = null,
    val format: String? = null,
    val url: String,
    val isDrm: Boolean = false,
    val headers: Map<String, String> = emptyMap()
)

data class Subtitle(
    val lang: String? = null,
    val label: String? = null,
    val url: String
)
