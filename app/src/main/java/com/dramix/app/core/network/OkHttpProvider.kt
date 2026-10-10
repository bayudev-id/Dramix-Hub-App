package com.dramix.app.core.network

import android.content.Context
import com.dramix.app.BuildConfig
import com.dramix.app.core.security.DeviceIdentifier
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.net.Inet4Address
import java.util.concurrent.TimeUnit

object OkHttpProvider {

    /**
     * Custom DNS that prioritizes IPv4 addresses before IPv6.
     * Prevents multi-second delays on dual-stack CDNs (such as AWS CloudFront for WeTV)
     * when the local network advertises IPv6 but has no routable IPv6 WAN connection.
     */
    private val ipv4FirstDns = object : Dns {
        override fun lookup(hostname: String): List<java.net.InetAddress> {
            val addresses = Dns.SYSTEM.lookup(hostname)
            return if (addresses.size <= 1) {
                addresses
            } else {
                addresses.sortedWith(compareBy { it !is Inet4Address })
            }
        }
    }

    /**
     * Dedicated high-concurrency OkHttpClient for Coil image loading.
     *
     * - maxRequestsPerHost: 32 (default is 5). Prevents grid image loading from
     *   bottlenecking when 20+ covers are fetched concurrently from the same CDN host.
     * - connectionPool: 32 connections kept alive to eliminate TLS renegotiation overhead.
     * - dns: ipv4FirstDns to prevent stalling on unreachable IPv6 addresses.
     * - No SecurityHeadersInterceptor or logging to maximize image fetch throughput.
     * - CdnRefererInterceptor preserved for MovieBox/Youku CDNs.
     */
    fun createImageClient(context: Context): OkHttpClient {
        val dispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 16
        }

        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(ConnectionPool(32, 2, TimeUnit.MINUTES))
            .dns(ipv4FirstDns)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(CdnRefererInterceptor())
            .build()
    }

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
            .dns(ipv4FirstDns)
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
