package org.rfcx.companion.repo

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import org.rfcx.companion.entity.request.DeploymentRequest
import org.rfcx.companion.entity.request.EditDeploymentRequest
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

    @GET("deployments")
    fun getDeployments(
        @Header("Authorization") authUser: String,
        @Query("active") active: Boolean = true
    ): Call<List<DeploymentResponse>>

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
    fun uploadImage(
        @Header("Authorization") authUser: String,
        @Path("id") id: String,
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>

    @GET("streams")
    fun getStreams(@Header("Authorization") authUser: String): Call<List<StreamResponse>>

    @GET("projects")
    fun getProjects(@Header("Authorization") authUser: String): Call<List<ProjectResponse>>

}
