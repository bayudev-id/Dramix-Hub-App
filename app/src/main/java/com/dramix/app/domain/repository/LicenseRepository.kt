package com.dramix.app.domain.repository

import com.dramix.app.domain.model.LicenseStatus
import kotlinx.coroutines.flow.Flow

interface LicenseRepository {
    suspend fun activateLicense(licenseKey: String): Result<LicenseStatus>
    suspend fun refreshLicenseStatus(): Result<LicenseStatus>
    fun isVipActive(): Boolean
    fun getSessionToken(): String?
    fun observeVipStatus(): Flow<Boolean>
    fun clearLicense()
}
