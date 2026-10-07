package com.dramix.app.data.source.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ActivateLicenseRequestDto(
    @Json(name = "license_key") val licenseKey: String,
    @Json(name = "device_id") val deviceId: String
)

@JsonClass(generateAdapter = true)
data class LicenseStatusDto(
    @Json(name = "is_vip") val isVip: Boolean = false,
    @Json(name = "license_key") val licenseKey: String? = null,
    @Json(name = "token") val token: String? = null,
    @Json(name = "expires_at") val expiresAt: Long? = null,
    @Json(name = "plan_name") val planName: String? = null
)
