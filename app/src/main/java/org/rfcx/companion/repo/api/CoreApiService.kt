package org.rfcx.companion.repo.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url

interface CoreApiService {

    @GET
    @Headers("No-Authentication: true")
    fun downloadFile(@Url url: String): Call<ResponseBody>
}
