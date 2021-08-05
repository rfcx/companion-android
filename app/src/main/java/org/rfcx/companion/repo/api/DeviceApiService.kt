package org.rfcx.companion.repo.api

import org.rfcx.companion.entity.response.DeploymentAssetResponse
import org.rfcx.companion.entity.response.ProjectResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

//TODO: Convert Old device api to MVVM pattern

interface DeviceApiService {
    @GET("projects")
    fun getProjects(
        @Header("Authorization") authUser: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("fields") fields: List<String> = listOf("id", "name", "isPublic", "externalId", "permissions")
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
}
