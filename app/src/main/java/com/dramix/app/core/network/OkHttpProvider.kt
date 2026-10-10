package com.dramix.app.core.network

import android.content.Context
import com.dramix.app.BuildConfig
import com.dramix.app.core.security.DeviceIdentifier
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

object OkHttpProvider {

    fun createClient(
        context: Context? = null,
        deviceIdentifier: DeviceIdentifier,
        enablePinning: Boolean = !BuildConfig.DEBUG
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS  // HEADERS instead of BODY to avoid logging image binary
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(CdnRefererInterceptor())
            .addInterceptor(SecurityHeadersInterceptor(deviceIdentifier))
            .addInterceptor(loggingInterceptor)

        // Add HTTP response cache (50MB) for image CDN caching
        if (context != null) {
            val cacheDir = File(context.cacheDir, "okhttp_cache")
            val cache = Cache(cacheDir, 50L * 1024 * 1024) // 50MB
            builder.cache(cache)
        }

        if (enablePinning) {
            // Certificate pinning for production Dramix Gateway domains
            val certificatePinner = CertificatePinner.Builder()
                .add("gateway.dramix.app", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .build()
            builder.certificatePinner(certificatePinner)
        }

        return builder.build()
    }
}
