package com.dramix.app.core.image

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient

object CoilProvider {

    /**
     * Global Coil ImageLoader for all cover/thumbnail rendering.
     *
     * - crossfade: short (250ms) fade-in for smooth poster swaps.
     * - respectCacheHeaders(false): cache aggressively regardless of what the
     *   WeTV/Viu/MovieBox CDNs return in their Cache-Control headers.
     * - Client-side downsampling: Coil's AsyncImage measures the composable and
     *   sets a pixel size target, so posters are decoded as small bitmaps.
     * - RGB_565: halves memory for opaque bitmaps.
     * - The OkHttpClient keeps CdnRefererInterceptor so MovieBox covers still
     *   send the required Referer header (prevents HTTP 429).
     */
    fun createImageLoader(
        context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(durationMillis = 250)
            .respectCacheHeaders(false)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)   // 25% of available app memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)   // 2% of storage
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .allowRgb565(true)
            .okHttpClient(okHttpClient)
            .build()
    }
}