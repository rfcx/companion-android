package org.rfcx.companion.repo

import org.rfcx.companion.entity.UserTouchResponse
import org.rfcx.companion.entity.response.ProjectByIdResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ApiRestInterface {
    @GET("v1/users/touchapi")
    fun userTouch(@Header("Authorization") authUser: String): Call<UserTouchResponse>

    @GET("projects/{id}")
    fun getProjectsById(
        @Header("Authorization") authUser: String,
        @Path("id") id: String
    ): Call<ProjectByIdResponse>
}
