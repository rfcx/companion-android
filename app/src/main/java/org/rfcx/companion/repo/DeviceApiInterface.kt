package org.rfcx.companion.repo

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import org.rfcx.companion.entity.request.DeploymentRequest
import org.rfcx.companion.entity.request.EditDeploymentRequest
import org.rfcx.companion.entity.request.GuardianDeploymentRequest
import org.rfcx.companion.entity.response.DeploymentAssetResponse
import org.rfcx.companion.entity.response.DeploymentResponse
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.entity.response.StreamResponse
import retrofit2.Call
import retrofit2.http.*

interface DeviceApiInterface {
    //deployment
    @POST("deployments")
    fun createDeployment(
        @Header("Authorization") authUser: String,
        @Body deploymentRequest: DeploymentRequest
    ): Call<ResponseBody>

    @POST("deployments")
    fun createGuardianDeployment(
        @Header("Authorization") authUser: String,
        @Body deploymentRequest: GuardianDeploymentRequest
    ): Call<ResponseBody>

    @GET("deployments")
    fun getDeployments(
        @Header("Authorization") authUser: String,
        @Query("active") active: Boolean = true,
        @Query("limit") limit: Int = 200
    ): Call<List<DeploymentResponse>>

    @GET("deployments/{id}")
    fun getDeployment(
        @Header("Authorization") authUser: String,
        @Path("id") id: String
    ): Call<DeploymentResponse>

    @PATCH("deployments/{id}")
    fun editDeployments(
        @Header("Authorization") authUser: String,
        @Path("id") id: String,
        @Body editDeploymentRequest: EditDeploymentRequest
    ): Call<ResponseBody>

    @DELETE("deployments/{id}")
    fun deleteDeployments(
        @Header("Authorization") authUser: String,
        @Path("id") id: String
    ): Call<ResponseBody>

    @Multipart
    @POST("deployments/{id}/assets")
    fun uploadAssets(
        @Header("Authorization") authUser: String,
        @Path("id") id: String,
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>

    @GET("deployments/{id}/assets")
    fun getDeploymentAssets(
        @Header("Authorization") authUser: String,
        @Path("id") id: String
    ): Call<List<DeploymentAssetResponse>>

    @GET("streams/{id}/assets")
    fun getStreamAssets(
        @Header("Authorization") authUser: String,
        @Path("id") id: String
    ): Call<List<DeploymentAssetResponse>>

    @Streaming
    @GET("assets/{id}")
    fun getGeoJsonFile(
        @Header("Authorization") authUser: String,
        @Path("id") id: String
    ): Call<ResponseBody>

    @GET("streams")
    fun getStreams(@Header("Authorization") authUser: String,
                   @Query("limit") limit: Int = 100,
                   @Query("offset") offset: Int = 0,
                   @Query("updated_after", encoded = true) updatedAfter: String? = null,
                   @Query("sort", encoded = true) sort: String? = null): Call<List<StreamResponse>>

    @GET("projects")
    fun getProjects(@Header("Authorization") authUser: String,
                    @Query("limit") limit: Int = 100,
                    @Query("offset") offset: Int = 0): Call<List<ProjectResponse>>

}
