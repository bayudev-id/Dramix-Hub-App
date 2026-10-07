package com.dramix.app.data.source.remote

import com.dramix.app.data.source.remote.dto.ActivateLicenseRequestDto
import com.dramix.app.data.source.remote.dto.GatewayResponse
import com.dramix.app.data.source.remote.dto.LicenseStatusDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LicenseApiService {

    @POST("api/license/activate")
    suspend fun activateLicense(
        @Body request: ActivateLicenseRequestDto
    ): GatewayResponse<LicenseStatusDto>

    @GET("api/license/status")
    suspend fun getLicenseStatus(
        @Query("device_id") deviceId: String
    ): GatewayResponse<LicenseStatusDto>
}
