package org.rfcx.companion.repo.api

import okhttp3.ResponseBody
import org.rfcx.companion.entity.UserTouchResponse
import org.rfcx.companion.entity.request.GuardianRegisterRequest
import org.rfcx.companion.entity.response.*
import retrofit2.Call
import retrofit2.http.*

// TODO: Convert Old device api to MVVM pattern

interface DeviceApiService {
    @GET("projects")
    fun getProjects(
        @Header("Authorization") authUser: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("fields") fields: List<String> = listOf("id", "name", "permissions")
    ): Call<List<ProjectResponse>>

    @GET("projects")
    fun getDeletedProjects(
        @Header("Authorization") authUser: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("only_deleted") onlyDeleted: Boolean = true,
        @Query("fields") fields: List<String> = listOf("id")
    ): Call<List<ProjectResponse>>

    @GET("streams/{id}/assets")
    fun getStreamAssets(
        @Header("Authorization") authUser: String,
        @Path("id") id: String
    ): Call<List<DeploymentAssetResponse>>

    @GET("usertouch")
    fun userTouch(@Header("Authorization") authUser: String): Call<UserTouchResponse>

    @GET("projects/{id}")
    fun getProjectsById(
        @Header("Authorization") authUser: String,
        @Path("id") id: String
    ): Call<ProjectByIdResponse>

    @POST("guardians")
    fun registerGuardian(
        @Header("Authorization") authUser: String,
        @Body guid: GuardianRegisterRequest
    ): Call<GuardianRegisterResponse>

    @GET("classifiers")
    fun checkAvailableClassifiers(
        @Header("Authorization") authUser: String
    ): Call<List<GuardianClassifierResponse>>

    @Streaming
    @GET
    fun downloadFile(@Url url: String): Call<ResponseBody>
}
