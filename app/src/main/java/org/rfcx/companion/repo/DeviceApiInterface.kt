package org.rfcx.companion.repo

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.rfcx.companion.entity.request.DeploymentRequest
import org.rfcx.companion.entity.request.EditDeploymentRequest
import org.rfcx.companion.entity.response.DeploymentAssetResponse
import org.rfcx.companion.entity.response.DeploymentResponse
import org.rfcx.companion.entity.response.StreamResponse
import retrofit2.Call
import retrofit2.http.*

interface DeviceApiInterface {
    // deployment
    @POST("deployments")
    fun createDeployment(
        @Body deploymentRequest: DeploymentRequest
    ): Call<ResponseBody>

    @GET("deployments")
    fun getDeployments(
        @Query("active") active: Boolean = true,
        @Query("limit") limit: Int = 200
    ): Call<List<DeploymentResponse>>

    @GET("deployments/{id}")
    fun getDeployment(
        @Path("id") id: String
    ): Call<DeploymentResponse>

    @PATCH("deployments/{id}")
    fun editDeployments(
        @Path("id") id: String,
        @Body editDeploymentRequest: EditDeploymentRequest
    ): Call<ResponseBody>

    @DELETE("deployments/{id}")
    fun deleteDeployments(
        @Path("id") id: String
    ): Call<ResponseBody>

    @Multipart
    @POST("deployments/{id}/assets")
    fun uploadAssets(
        @Path("id") id: String,
        @Part file: MultipartBody.Part,
        @Part("meta") params: RequestBody? = null,
    ): Call<ResponseBody>

    @GET("deployments/{id}/assets")
    fun getDeploymentAssets(
        @Path("id") id: String
    ): Call<List<DeploymentAssetResponse>>

    @GET("streams/{id}/assets")
    fun getStreamAssets(
        @Path("id") id: String
    ): Call<List<DeploymentAssetResponse>>

    @Streaming
    @GET("assets/{id}")
    fun getGeoJsonFile(
        @Path("id") id: String
    ): Call<ResponseBody>

    @GET("streams")
    fun getStreams(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("updated_after", encoded = true) updatedAfter: String? = null,
        @Query("sort", encoded = true) sort: String? = null,
        @Query("projects") projects: List<String>? = null
    ): Call<List<StreamResponse>>
}
