package org.rfcx.companion.repo.api

import okhttp3.ResponseBody
import org.rfcx.companion.entity.UserTouchResponse
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

    @Streaming
    @GET
    @Headers("No-Authentication: true")
    fun downloadFile(@Url url: String): Call<ResponseBody>
}
