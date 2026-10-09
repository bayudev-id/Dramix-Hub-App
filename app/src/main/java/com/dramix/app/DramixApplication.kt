package com.dramix.app

import android.app.Application
import coil.Coil
import com.dramix.app.core.image.CoilProvider
import com.dramix.app.di.appModules
import okhttp3.OkHttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DramixApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger(Level.ERROR)
                androidContext(this@DramixApplication)
                modules(appModules)
            }
        }

        // Initialize global Coil ImageLoader with CdnRefererInterceptor and caching
        val okHttpClient: OkHttpClient = get()
        val imageLoader = CoilProvider.createImageLoader(this, okHttpClient)
        Coil.setImageLoader(imageLoader)
    }
}
