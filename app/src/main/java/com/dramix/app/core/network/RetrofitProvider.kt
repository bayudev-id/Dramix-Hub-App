package com.dramix.app.core.network

import android.os.Build
import com.dramix.app.data.source.remote.GatewayApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitProvider {

    val DEFAULT_GATEWAY_URL: String
        get() {
            val isEmulator = (Build.FINGERPRINT.startsWith("generic") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu"))
            return if (isEmulator) "http://10.0.2.2:8090/" else "http://127.0.0.1:8090/"
        }

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

    fun createLicenseService(
        okHttpClient: OkHttpClient,
        baseUrl: String = DEFAULT_GATEWAY_URL,
        moshi: Moshi = createMoshi()
    ): com.dramix.app.data.source.remote.LicenseApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(com.dramix.app.data.source.remote.LicenseApiService::class.java)
    }
}
