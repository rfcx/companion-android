package org.rfcx.companion.repo

import org.rfcx.companion.entity.UserTouchResponse
import org.rfcx.companion.entity.request.GuardianRegisterRequest
import org.rfcx.companion.entity.response.ProjectByIdResponse
import org.rfcx.companion.entity.response.GuardianRegisterResponse
import org.rfcx.companion.entity.response.GuardianSoftwareResponse
import retrofit2.Call
import retrofit2.http.*

interface ApiRestInterface {
    @GET("v1/users/touchapi")
    fun userTouch(@Header("Authorization") authUser: String): Call<UserTouchResponse>

    @GET("projects/{id}")
    fun getProjectsById(
        @Header("Authorization") authUser: String,
        @Path("id") id: String
    ): Call<ProjectByIdResponse>

    @POST("v2/guardians/register")
    fun registerGuardian(
        @Header("Authorization") authUser: String,
        @Body guid: GuardianRegisterRequest
    ): Call<GuardianRegisterResponse>

    @GET("v2/guardians/u60oxzgwk6vf/software/all")
    fun checkSoftwareVersion(
        @Header("Authorization") authUser: String
    ): Call<List<GuardianSoftwareResponse>>
}
