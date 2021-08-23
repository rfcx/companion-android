package org.rfcx.companion.repo.api

import org.rfcx.companion.entity.response.GuardianSoftwareResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

interface CoreApiService {
    @GET("v2/guardians/u60oxzgwk6vf/software/all")
    fun checkSoftwareVersion(
        @Header("Authorization") authUser: String
    ): Call<List<GuardianSoftwareResponse>>
}
