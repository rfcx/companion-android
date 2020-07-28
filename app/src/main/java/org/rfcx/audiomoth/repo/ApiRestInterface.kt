package org.rfcx.audiomoth.repo

import org.rfcx.audiomoth.entity.UserTouchResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiRestInterface {
    @GET("v1/users/touchapi")
    fun userTouch(@Header("Authorization") authUser: String): Call<UserTouchResponse>
}
