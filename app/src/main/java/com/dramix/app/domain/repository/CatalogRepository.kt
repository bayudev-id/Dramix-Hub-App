package com.dramix.app.domain.repository

import com.dramix.app.domain.model.Category
import com.dramix.app.domain.model.DramaDetail
import com.dramix.app.domain.model.PlaybackSource
import com.dramix.app.domain.model.ProviderModel
import com.dramix.app.domain.model.VideoFeedPage
import com.dramix.app.domain.model.VideoItem

interface CatalogRepository {
    suspend fun getProviders(): Result<List<ProviderModel>>
    suspend fun getCategories(modelId: String): Result<List<Category>>
    suspend fun getVideos(modelId: String, categoryId: String, page: Int = 1): Result<List<VideoItem>>
    suspend fun getVideoFeed(modelId: String, categoryId: String, page: Int = 1): Result<VideoFeedPage> =
        getVideos(modelId, categoryId, page).map { VideoFeedPage(items = it, hasMore = it.size >= 10) }
    suspend fun getDramaDetail(modelId: String, id: String): Result<DramaDetail>
    suspend fun getPlaybackSource(modelId: String, episodeId: String, id: String? = null): Result<PlaybackSource>
    suspend fun search(modelId: String, query: String, page: Int = 1, contentType: String? = null): Result<List<VideoItem>>
}
