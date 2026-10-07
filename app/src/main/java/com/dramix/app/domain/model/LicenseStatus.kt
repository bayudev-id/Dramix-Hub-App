package com.dramix.app.domain.model

data class LicenseStatus(
    val isVip: Boolean = false,
    val licenseKey: String? = null,
    val token: String? = null,
    val expiresAt: Long? = null,
    val planName: String? = null
)
