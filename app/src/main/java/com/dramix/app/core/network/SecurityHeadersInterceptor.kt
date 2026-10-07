package com.dramix.app.core.network

import com.dramix.app.core.security.DeviceIdentifier
import okhttp3.Interceptor
import okhttp3.Response

class SecurityHeadersInterceptor(
    private val deviceIdentifier: DeviceIdentifier
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("X-Device-Id", deviceIdentifier.getDeviceId())
            .header("X-Timestamp", System.currentTimeMillis().toString())
            .header("User-Agent", "Dramix/1.0.0 (Android; Client)")

        return chain.proceed(requestBuilder.build())
    }
}
