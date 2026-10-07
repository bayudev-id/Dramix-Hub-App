package com.dramix.app.data.source.remote

import com.dramix.app.data.source.remote.dto.CategoryDto
import com.dramix.app.data.source.remote.dto.DramaDetailDataDto
import com.dramix.app.data.source.remote.dto.GatewayResponse
import com.dramix.app.data.source.remote.dto.PlaybackSourceDataDto
import com.dramix.app.data.source.remote.dto.ProviderModelDto
import com.dramix.app.data.source.remote.dto.SearchDataDto
import com.dramix.app.data.source.remote.dto.SearchRequestDto
import com.dramix.app.data.source.remote.dto.VideoFeedDataDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GatewayApiService {

    @GET("api/modelles/models")
    suspend fun getModels(): GatewayResponse<List<ProviderModelDto>>

    @GET("api/modelles/categories")
    suspend fun getCategories(
        @Query("model_id") modelId: String
    ): GatewayResponse<List<CategoryDto>>

    @GET("api/modelles/videos")
    suspend fun getVideos(
        @Query("model_id") modelId: String,
        @Query("category_id") categoryId: String,
        @Query("page") page: Int = 1
    ): GatewayResponse<VideoFeedDataDto>

    @GET("api/modelles/detail")
    suspend fun getDetail(
        @Query("model_id") modelId: String,
        @Query("id") id: String
    ): GatewayResponse<DramaDetailDataDto>

    @GET("api/modelles/source")
    suspend fun getSource(
        @Query("model_id") modelId: String,
        @Query("episode_id") episodeId: String,
        @Query("id") id: String? = null
    ): GatewayResponse<PlaybackSourceDataDto>

    @GET("api/modelles/search")
    suspend fun search(
        @Query("model_id") modelId: String,
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("content_type") contentType: String? = null
    ): GatewayResponse<SearchDataDto>

    @POST("api/modelles/search")
    suspend fun searchWithBody(
        @Body request: SearchRequestDto
    ): GatewayResponse<SearchDataDto>
}
