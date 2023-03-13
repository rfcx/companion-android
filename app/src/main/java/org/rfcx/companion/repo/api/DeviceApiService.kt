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
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("fields") fields: List<String> = listOf("id", "name", "permissions")
    ): Call<List<ProjectResponse>>

    @GET("projects")
    fun getDeletedProjects(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("only_deleted") onlyDeleted: Boolean = true,
        @Query("fields") fields: List<String> = listOf("id")
    ): Call<List<ProjectResponse>>

    @GET("projects/{id}/offtimes")
    fun getProjectOffTime(
        @Path("id") id: String
    ): Call<ProjectOffTimeResponse>

    @GET("streams/{id}/assets")
    fun getStreamAssets(
        @Path("id") id: String
    ): Call<List<DeploymentAssetResponse>>

    @GET("usertouch")
    fun userTouch(): Call<UserTouchResponse>

    @GET("projects/{id}")
    fun getProjectsById(
        @Path("id") id: String
    ): Call<ProjectByIdResponse>

    @POST("guardians")
    fun registerGuardian(
        @Body guid: GuardianRegisterRequest
    ): Call<GuardianRegisterResponse>

    @GET("classifiers")
    fun checkAvailableClassifiers(): Call<List<GuardianClassifierResponse>>

    @Streaming
    @GET
    fun downloadFile(@Url url: String): Call<ResponseBody>
}
