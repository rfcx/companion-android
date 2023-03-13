package org.rfcx.companion.repo.api

import okhttp3.ResponseBody
import org.rfcx.companion.entity.response.GuardianSoftwareResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url

interface CoreApiService {
    @GET("v2/guardians/software/all")
    fun checkSoftwareVersion(): Call<List<GuardianSoftwareResponse>>

    @GET
    @Headers("No-Authentication: true")
    fun downloadFile(@Url url: String): Call<ResponseBody>
}
