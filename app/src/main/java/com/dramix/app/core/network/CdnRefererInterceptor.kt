package com.dramix.app.core.network

import okhttp3.Interceptor
import okhttp3.Response

class CdnRefererInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host.lowercase()

        val requestBuilder = request.newBuilder()

        // MovieBox CDN (hakunaymatata / aoneroom) requires strict Referer to prevent HTTP 429
        if (host.endsWith("hakunaymatata.com") || host.endsWith("aoneroom.com")) {
            val currentReferer = request.header("Referer")
            if (currentReferer.isNullOrBlank() || currentReferer.equals("https://dramix.app/", ignoreCase = true)) {
                requestBuilder.header("Referer", "https://moviebox.ph/")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
