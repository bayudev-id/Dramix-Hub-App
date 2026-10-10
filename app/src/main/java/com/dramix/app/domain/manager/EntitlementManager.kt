package com.dramix.app.domain.manager

import com.dramix.app.domain.repository.LicenseRepository

sealed class PlaybackAccess {
    data class AccessGranted(val isFree: Boolean) : PlaybackAccess()
    data class AccessDenied(val reason: String) : PlaybackAccess()
}

class EntitlementManager(
    private val licenseRepository: LicenseRepository
) {

    fun canPlayEpisode(
        episodeNumber: Int,
        isVipEpisode: Boolean = false
    ): PlaybackAccess {
        val hasActiveLicense = licenseRepository.isVipActive()

        // If the episode explicitly requires VIP
        if (isVipEpisode) {
            return if (hasActiveLicense) {
                PlaybackAccess.AccessGranted(isFree = false)
            } else {
                PlaybackAccess.AccessDenied(REASON_REQUIRE_VIP_LICENSE)
            }
        }

        // Freemium Rule: Episode 0..3 are free (including prologue/special ep 0)
        return if (episodeNumber in 0..3) {
            PlaybackAccess.AccessGranted(isFree = true)
        } else {
            // Episode 4+ requires active VIP license
            if (hasActiveLicense) {
                PlaybackAccess.AccessGranted(isFree = false)
            } else {
                PlaybackAccess.AccessDenied(REASON_REQUIRE_VIP_LICENSE)
            }
        }
    }

    companion object {
        const val REASON_REQUIRE_VIP_LICENSE = "REQUIRE_VIP_LICENSE"
    }
}
