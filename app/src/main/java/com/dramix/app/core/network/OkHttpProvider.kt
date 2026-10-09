package com.dramix.app.core.network

import com.dramix.app.BuildConfig
import com.dramix.app.core.security.DeviceIdentifier
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object OkHttpProvider {

    fun createClient(
        deviceIdentifier: DeviceIdentifier,
        enablePinning: Boolean = !BuildConfig.DEBUG
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
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
