package com.dramix.app.domain.model

data class ProviderModel(
    val id: String,
    val name: String,
    val iconUrl: String? = null,
    val description: String? = null,
    val contentType: String = "long_drama",
    val status: String = "active"
) {
    val isActive: Boolean
        get() = status.equals("active", ignoreCase = true)
}

data class Category(
    val id: String,
    val name: String
)

data class VideoFeedPage(
    val items: List<VideoItem> = emptyList(),
    val hasMore: Boolean = false
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
) {
    val isSewa: Boolean
        get() {
            val isWetvSource = source == null || source.equals("wetv", ignoreCase = true)
            if (!isWetvSource) return false
            return tags.any { it.equals("sewa", ignoreCase = true) || it.equals("rent", ignoreCase = true) }
        }
}

data class Dub(
    val id: String,
    val title: String,
    val name: String = title,
    val lanCode: String? = null,
    val isOriginal: Boolean = false
)

data class CastMember(
    val id: String = "",
    val name: String = "",
    val role: String? = null,
    val cover: String? = null,
    val isDirector: Boolean = false
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
    val seasons: List<Season> = emptyList(),
    val dubs: List<Dub> = emptyList(),
    val cast: List<CastMember> = emptyList()
) {
    val isSewa: Boolean
        get() {
            val isWetvSource = source == null || source.equals("wetv", ignoreCase = true)
            if (!isWetvSource) return false
            return tags.any { it.equals("sewa", ignoreCase = true) || it.equals("rent", ignoreCase = true) }
        }
}

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
) {
    val isSewa: Boolean
        get() {
            if (label?.equals("sewa", ignoreCase = true) == true) return true
            if (label?.equals("rent", ignoreCase = true) == true) return true
            return tags.any { it.equals("sewa", ignoreCase = true) || it.equals("rent", ignoreCase = true) }
        }
}

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
