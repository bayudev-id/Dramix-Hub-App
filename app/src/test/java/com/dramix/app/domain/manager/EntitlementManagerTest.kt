package com.dramix.app.domain.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dramix.app.core.security.DeviceIdentifier
import com.dramix.app.data.repository.LicenseRepositoryImpl
import com.dramix.app.data.source.local.LicensePreferences
import com.dramix.app.data.source.remote.LicenseApiService
import com.dramix.app.data.source.remote.dto.ActivateLicenseRequestDto
import com.dramix.app.data.source.remote.dto.GatewayResponse
import com.dramix.app.data.source.remote.dto.LicenseStatusDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EntitlementManagerTest {

    private lateinit var licensePreferences: LicensePreferences
    private lateinit var entitlementManager: EntitlementManager
    private lateinit var licenseRepository: LicenseRepositoryImpl

    private class FakeLicenseApiService : LicenseApiService {
        var shouldSucceed = true

        override suspend fun activateLicense(request: ActivateLicenseRequestDto): GatewayResponse<LicenseStatusDto> {
            return if (shouldSucceed) {
                GatewayResponse(
                    code = 200,
                    message = "success",
                    data = LicenseStatusDto(
                        isVip = true,
                        licenseKey = request.licenseKey,
                        token = "session_token_abc_123",
                        expiresAt = System.currentTimeMillis() + 86400000L,
                        planName = "VIP 1 Bulan"
                    )
                )
            } else {
                GatewayResponse(code = 400, message = "invalid key", data = null)
            }
        }

        override suspend fun getLicenseStatus(deviceId: String): GatewayResponse<LicenseStatusDto> {
            return GatewayResponse(
                code = 200,
                message = "success",
                data = LicenseStatusDto(
                    isVip = true,
                    licenseKey = "LCN-SELLER-123",
                    token = "session_token_abc_123",
                    expiresAt = System.currentTimeMillis() + 86400000L,
                    planName = "VIP 1 Bulan"
                )
            )
        }
    }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        licensePreferences = LicensePreferences(context)
        licensePreferences.clearLicense()

        val deviceIdentifier = DeviceIdentifier(context)
        val apiService = FakeLicenseApiService()

        licenseRepository = LicenseRepositoryImpl(
            apiService = apiService,
            licensePreferences = licensePreferences,
            deviceIdentifier = deviceIdentifier
        )
        entitlementManager = EntitlementManager(licenseRepository)
    }

    @Test
    fun freemium_policy_allows_episodes_1_to_3_without_license() {
        // Episode 1 -> Granted Free
        val accessEp1 = entitlementManager.canPlayEpisode(episodeNumber = 1)
        assertTrue(accessEp1 is PlaybackAccess.AccessGranted)
        assertTrue((accessEp1 as PlaybackAccess.AccessGranted).isFree)

        // Episode 2 -> Granted Free
        val accessEp2 = entitlementManager.canPlayEpisode(episodeNumber = 2)
        assertTrue(accessEp2 is PlaybackAccess.AccessGranted)
        assertTrue((accessEp2 as PlaybackAccess.AccessGranted).isFree)

        // Episode 3 -> Granted Free
        val accessEp3 = entitlementManager.canPlayEpisode(episodeNumber = 3)
        assertTrue(accessEp3 is PlaybackAccess.AccessGranted)
        assertTrue((accessEp3 as PlaybackAccess.AccessGranted).isFree)
    }

    @Test
    fun episode_4_denied_without_active_license() {
        val accessEp4 = entitlementManager.canPlayEpisode(episodeNumber = 4)
        assertTrue(accessEp4 is PlaybackAccess.AccessDenied)
        assertEquals(
            EntitlementManager.REASON_REQUIRE_VIP_LICENSE,
            (accessEp4 as PlaybackAccess.AccessDenied).reason
        )
    }

    @Test
    fun episode_4_granted_when_license_activated() = runTest {
        // Activate license
        val activateResult = licenseRepository.activateLicense("LCN-SELLER-TEST-1")
        assertTrue(activateResult.isSuccess)
        assertTrue(licenseRepository.isVipActive())

        val accessEp4 = entitlementManager.canPlayEpisode(episodeNumber = 4)
        assertTrue(accessEp4 is PlaybackAccess.AccessGranted)
        assertFalse((accessEp4 as PlaybackAccess.AccessGranted).isFree)
    }

    @Test
    fun expired_license_denies_episode_4() {
        // Save expired license (1 hour in the past)
        licensePreferences.saveLicense(
            isVip = true,
            token = "expired_token",
            expiresAt = System.currentTimeMillis() - 3600000L,
            licenseKey = "LCN-EXPIRED",
            planName = "Expired Plan"
        )

        assertFalse(licenseRepository.isVipActive())
        val accessEp4 = entitlementManager.canPlayEpisode(episodeNumber = 4)
        assertTrue(accessEp4 is PlaybackAccess.AccessDenied)
    }

    @Test
    fun explicit_vip_episode_1_requires_active_license() {
        val access = entitlementManager.canPlayEpisode(episodeNumber = 1, isVipEpisode = true)
        assertTrue(access is PlaybackAccess.AccessDenied)
    }

    @Test
    fun license_preferences_flow_updates_reactively() = runTest {
        assertFalse(licenseRepository.observeVipStatus().first())

        licensePreferences.saveLicense(
            isVip = true,
            token = "tok",
            expiresAt = System.currentTimeMillis() + 100000L,
            licenseKey = "key",
            planName = "plan"
        )

        assertTrue(licenseRepository.observeVipStatus().first())
    }
}
