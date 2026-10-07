package com.dramix.app.di

import com.dramix.app.core.database.AppDatabase
import com.dramix.app.core.network.AuthInterceptor
import com.dramix.app.core.network.OkHttpProvider
import com.dramix.app.core.network.RetrofitProvider
import com.dramix.app.core.security.DeviceIdentifier
import com.dramix.app.core.security.SecurityManager
import com.dramix.app.data.repository.CatalogRepositoryImpl
import com.dramix.app.data.repository.LicenseRepositoryImpl
import com.dramix.app.data.source.local.LicensePreferences
import com.dramix.app.domain.manager.EntitlementManager
import com.dramix.app.domain.repository.CatalogRepository
import com.dramix.app.domain.repository.LicenseRepository
import com.dramix.app.player.engine.PlayerFactory
import com.dramix.app.ui.screens.home.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single { AppDatabase.getInstance(androidContext()) }
    single { get<AppDatabase>().watchHistoryDao() }
    single { get<AppDatabase>().bookmarkDao() }
    single { get<AppDatabase>().downloadRecordDao() }
}

val coreModule = module {
    single { DeviceIdentifier(androidContext()) }
    single { SecurityManager() }
    single { LicensePreferences(androidContext()) }

    single {
        val baseClient = OkHttpProvider.createClient(deviceIdentifier = get())
        val authInterceptor = AuthInterceptor(licensePreferences = get())
        baseClient.newBuilder()
            .addInterceptor(authInterceptor)
            .build()
    }

    single { RetrofitProvider.createGatewayService(okHttpClient = get()) }
    single { RetrofitProvider.createLicenseService(okHttpClient = get()) }
}

val repositoryModule = module {
    single<CatalogRepository> { CatalogRepositoryImpl(apiService = get()) }
    single<LicenseRepository> {
        LicenseRepositoryImpl(
            apiService = get(),
            licensePreferences = get(),
            deviceIdentifier = get()
        )
    }
    single { EntitlementManager(licenseRepository = get()) }
    single { PlayerFactory(context = androidContext(), okHttpClient = get()) }
}

val viewModelModule = module {
    viewModel {
        HomeViewModel(
            catalogRepository = get(),
            watchHistoryDao = get()
        )
    }
}

val appModules = listOf(
    databaseModule,
    coreModule,
    repositoryModule,
    viewModelModule
)
