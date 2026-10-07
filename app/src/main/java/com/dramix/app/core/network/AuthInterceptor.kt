package com.dramix.app.core.network

import com.dramix.app.data.source.local.LicensePreferences
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val licensePreferences: LicensePreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = licensePreferences.getToken()

        val newRequest = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}
