package com.dramix.app.data.repository

import com.dramix.app.core.security.DeviceIdentifier
import com.dramix.app.data.source.local.LicensePreferences
import com.dramix.app.data.source.remote.LicenseApiService
import com.dramix.app.data.source.remote.dto.ActivateLicenseRequestDto
import com.dramix.app.domain.model.LicenseStatus
import com.dramix.app.domain.repository.LicenseRepository
import kotlinx.coroutines.flow.Flow

class LicenseRepositoryImpl(
    private val apiService: LicenseApiService,
    private val licensePreferences: LicensePreferences,
    private val deviceIdentifier: DeviceIdentifier
) : LicenseRepository {

    override suspend fun activateLicense(licenseKey: String): Result<LicenseStatus> = runCatching {
        val request = ActivateLicenseRequestDto(
            licenseKey = licenseKey.trim(),
            deviceId = deviceIdentifier.getDeviceId()
        )
        val response = apiService.activateLicense(request)
        val data = response.data ?: throw IllegalStateException("Activation failed: empty response")

        licensePreferences.saveLicense(
            isVip = data.isVip,
            token = data.token,
            expiresAt = data.expiresAt,
            licenseKey = data.licenseKey ?: licenseKey,
            planName = data.planName
        )

        LicenseStatus(
            isVip = data.isVip,
            licenseKey = data.licenseKey ?: licenseKey,
            token = data.token,
            expiresAt = data.expiresAt,
            planName = data.planName
        )
    }

    override suspend fun refreshLicenseStatus(): Result<LicenseStatus> = runCatching {
        val deviceId = deviceIdentifier.getDeviceId()
        val response = apiService.getLicenseStatus(deviceId)
        val data = response.data ?: throw IllegalStateException("Status check failed: empty response")

        licensePreferences.saveLicense(
            isVip = data.isVip,
            token = data.token,
            expiresAt = data.expiresAt,
            licenseKey = data.licenseKey ?: licensePreferences.getLicenseKey(),
            planName = data.planName
        )

        LicenseStatus(
            isVip = data.isVip,
            licenseKey = data.licenseKey ?: licensePreferences.getLicenseKey(),
            token = data.token,
            expiresAt = data.expiresAt,
            planName = data.planName
        )
    }

    override fun isVipActive(): Boolean = licensePreferences.isVipActive()

    override fun getSessionToken(): String? = licensePreferences.getToken()

    override fun observeVipStatus(): Flow<Boolean> = licensePreferences.vipStatusFlow

    override fun clearLicense() {
        licensePreferences.clearLicense()
    }
}
