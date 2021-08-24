package org.rfcx.companion.repo.api

import okhttp3.ResponseBody
import org.rfcx.companion.entity.response.GuardianSoftwareResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

interface CoreApiService {
    @GET("v2/guardians/u60oxzgwk6vf/software/all")
    fun checkSoftwareVersion(
        @Header("Authorization") authUser: String
    ): Call<List<GuardianSoftwareResponse>>

    @GET
    fun downloadAPK(@Url url: String): Call<ResponseBody>
}
