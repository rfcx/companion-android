package org.rfcx.companion.repo

import okhttp3.ResponseBody
import org.rfcx.companion.entity.request.DeploymentRequest
import org.rfcx.companion.entity.request.EditDeploymentRequest
import org.rfcx.companion.entity.response.DeploymentResponse
import retrofit2.Call
import retrofit2.http.*

interface DeviceApiInterface {
    @POST("deployments")
    fun createDeployment(
        @Header("Authorization") authUser: String,
        @Body deploymentRequest: DeploymentRequest
    ): Call<ResponseBody>

    @GET("deployments")
    fun getDeployments(@Header("Authorization") authUser: String): Call<List<DeploymentResponse>>

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

}
