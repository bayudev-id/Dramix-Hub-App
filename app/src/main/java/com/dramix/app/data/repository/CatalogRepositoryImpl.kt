package com.dramix.app.data.repository

import com.dramix.app.data.source.remote.GatewayApiService
import com.dramix.app.data.source.remote.dto.CastMemberDto
import com.dramix.app.data.source.remote.dto.CategoryDto
import com.dramix.app.data.source.remote.dto.DramaDetailDataDto
import com.dramix.app.data.source.remote.dto.DubDto
import com.dramix.app.data.source.remote.dto.EpisodeDto
import com.dramix.app.data.source.remote.dto.PlaybackSourceDataDto
import com.dramix.app.data.source.remote.dto.ProviderModelDto
import com.dramix.app.data.source.remote.dto.SeasonDto
import com.dramix.app.data.source.remote.dto.StreamDto
import com.dramix.app.data.source.remote.dto.SubtitleDto
import com.dramix.app.data.source.remote.dto.VideoItemDto
import com.dramix.app.domain.model.CastMember
import com.dramix.app.domain.model.Category
import com.dramix.app.domain.model.DramaDetail
import com.dramix.app.domain.model.Dub
import com.dramix.app.domain.model.Episode
import com.dramix.app.domain.model.PlaybackSource
import com.dramix.app.domain.model.ProviderModel
import com.dramix.app.domain.model.Season
import com.dramix.app.domain.model.StreamSource
import com.dramix.app.domain.model.Subtitle
import com.dramix.app.domain.model.VideoFeedPage
import com.dramix.app.domain.model.VideoItem
import com.dramix.app.domain.repository.CatalogRepository

class CatalogRepositoryImpl(
    private val apiService: GatewayApiService
) : CatalogRepository {

    override suspend fun getProviders(): Result<List<ProviderModel>> = runCatching {
        val response = apiService.getModels()
        (response.data ?: emptyList()).map { it.toDomain() }
    }

    override suspend fun getCategories(modelId: String): Result<List<Category>> = runCatching {
        val response = apiService.getCategories(modelId)
        (response.data?.data ?: emptyList()).map { it.toDomain() }
    }

    override suspend fun getVideos(
        modelId: String,
        categoryId: String,
        page: Int
    ): Result<List<VideoItem>> = runCatching {
        val response = apiService.getVideos(modelId, categoryId, page)
        (response.data?.items ?: emptyList()).map { it.toDomain() }
    }

    override suspend fun getVideoFeed(
        modelId: String,
        categoryId: String,
        page: Int
    ): Result<VideoFeedPage> = runCatching {
        val response = apiService.getVideos(modelId, categoryId, page)
        val data = response.data
        val items = (data?.items ?: emptyList()).map { it.toDomain() }
        val hasMore = data?.hasMore ?: false
        VideoFeedPage(items = items, hasMore = hasMore)
    }

    override suspend fun getDramaDetail(modelId: String, id: String): Result<DramaDetail> = runCatching {
        val response = apiService.getDetail(modelId, id)
        val data = response.data ?: throw IllegalStateException("Detail data is null for $id")
        val domain = data.toDomain()
        if (modelId.equals("kisskh", ignoreCase = true)) {
            val sortedSeasons = domain.seasons.map { season ->
                season.copy(
                    episodes = season.episodes.sortedWith(
                        compareBy<Episode> { it.number }.thenBy { it.id }
                    )
                )
            }
            domain.copy(seasons = sortedSeasons)
        } else {
            domain
        }
    }

    override suspend fun getPlaybackSource(
        modelId: String,
        episodeId: String,
        id: String?
    ): Result<PlaybackSource> = runCatching {
        val response = apiService.getSource(modelId, episodeId, id)
        val data = response.data ?: throw IllegalStateException("Playback source is null for $episodeId")
        data.toDomain(fallbackEpisodeId = episodeId)
    }

    override suspend fun search(
        modelId: String,
        query: String,
        page: Int,
        contentType: String?
    ): Result<List<VideoItem>> = runCatching {
        val response = apiService.search(modelId, query, page, contentType)
        (response.data?.items ?: emptyList()).map { it.toDomain() }
    }

    override suspend fun getSearchFeed(
        modelId: String,
        query: String,
        page: Int,
        contentType: String?
    ): Result<com.dramix.app.domain.model.SearchFeedPage> = runCatching {
        val response = apiService.search(modelId, query, page, contentType)
        val data = response.data
        val items = (data?.items ?: emptyList()).map { it.toDomain() }
        com.dramix.app.domain.model.SearchFeedPage(
            items = items,
            hasMore = data?.hasMore ?: false,
            currentPage = page
        )
    }

    private fun ProviderModelDto.toDomain() = ProviderModel(
        id = id,
        name = name,
        iconUrl = iconUrl,
        description = description,
        contentType = contentType ?: "long_drama",
        status = status ?: "active"
    )

    private fun CategoryDto.toDomain() = Category(
        id = id,
        name = name
    )

    private fun VideoItemDto.toDomain() = VideoItem(
        id = id,
        title = title,
        cover = cover,
        type = type,
        source = source,
        episodeInfo = episodeInfo,
        score = score,
        views = views,
        isVip = isVip,
        tags = tags ?: emptyList()
    )

    private fun DramaDetailDataDto.toDomain() = DramaDetail(
        id = id,
        title = title,
        cover = cover,
        description = description,
        type = type,
        source = source,
        releaseDate = releaseDate,
        score = score,
        views = views,
        isVip = isVip,
        tags = tags ?: emptyList(),
        totalEpisodes = totalEpisodes,
        seasons = seasons?.map { it.toDomain() } ?: emptyList(),
        dubs = dubs?.map { it.toDomain() } ?: emptyList(),
        cast = cast?.map { it.toDomain() } ?: emptyList()
    )

    private fun DubDto.toDomain() = Dub(
        id = id,
        title = title ?: name.orEmpty(),
        name = name ?: title.orEmpty(),
        lanCode = lanCode,
        isOriginal = isOriginal
    )

    private fun CastMemberDto.toDomain() = CastMember(
        id = id.orEmpty(),
        name = name.orEmpty(),
        role = role,
        cover = cover,
        isDirector = isDirector ?: false
    )

    private fun SeasonDto.toDomain() = Season(
        name = name,
        index = index,
        totalEpisodes = totalEpisodes,
        episodes = episodes?.map { it.toDomain() } ?: emptyList()
    )

    private fun EpisodeDto.toDomain() = Episode(
        id = id,
        title = title,
        number = number ?: 1,
        cover = cover,
        durationSeconds = durationSeconds,
        isVip = isVip,
        isExpress = isExpress,
        isTrailer = isTrailer,
        label = label,
        tags = tags ?: emptyList()
    )

    private fun PlaybackSourceDataDto.toDomain(fallbackEpisodeId: String): PlaybackSource {
        val rawStreams = streams ?: emptyList()
        val detectedCountdownStream = rawStreams.firstOrNull {
            it.format.equals("countdown", ignoreCase = true) ||
            it.url.contains("tickcounter.com", ignoreCase = true) ||
            it.url.contains("countdown", ignoreCase = true)
        }
        val finalIsCountdown = isCountdown || (detectedCountdownStream != null)
        val finalCountdownUrl = countdownUrl ?: detectedCountdownStream?.url

        val validStreams = rawStreams
            .filterNot {
                it.format.equals("countdown", ignoreCase = true) ||
                it.url.contains("tickcounter.com", ignoreCase = true) ||
                it.url.contains("countdown", ignoreCase = true)
            }
            .map { it.toDomain() }

        return PlaybackSource(
            id = id ?: fallbackEpisodeId,
            episodeId = episodeId ?: fallbackEpisodeId,
            durationSeconds = durationSeconds,
            streams = validStreams,
            subtitles = subtitles?.map { it.toDomain() } ?: emptyList(),
            headers = headers ?: emptyMap(),
            countdownUrl = finalCountdownUrl,
            isCountdown = finalIsCountdown
        )
    }

    private fun StreamDto.toDomain() = StreamSource(
        quality = quality,
        format = format,
        url = url,
        isDrm = isDrm,
        drm = drm?.let { d ->
            com.dramix.app.domain.model.DrmConfig(
                licenseUrl = d.licenseUrl,
                licenseMethod = d.licenseMethod,
                licenseParams = d.licenseParams ?: emptyMap(),
                type = d.type,
                systems = d.systems ?: emptyList()
            )
        },
        headers = headers ?: emptyMap()
    )

    private fun SubtitleDto.toDomain() = Subtitle(
        lang = lang,
        label = label,
        url = url
    )
}
