package com.dramix.app.core.network

import com.dramix.app.data.source.remote.GatewayApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitProvider {

    const val DEFAULT_GATEWAY_URL = "http://10.0.2.2:8090/"

    fun createMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun createGatewayService(
        okHttpClient: OkHttpClient,
        baseUrl: String = DEFAULT_GATEWAY_URL,
        moshi: Moshi = createMoshi()
    ): GatewayApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(GatewayApiService::class.java)
    }
}
