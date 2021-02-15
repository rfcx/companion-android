package org.rfcx.companion.repo

import okhttp3.ResponseBody
import org.rfcx.companion.entity.request.DeploymentRequest
import org.rfcx.companion.entity.response.DeploymentResponse
import org.rfcx.companion.entity.response.EdgeDeploymentResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface DeviceApiInterface {
    @POST("deployments")
    fun createDeployment(@Header("Authorization") authUser: String,
                         @Body deploymentRequest: DeploymentRequest): Call<ResponseBody>

    @GET("deployments")
    fun getDeployments(@Header("Authorization") authUser: String): Call<List<DeploymentResponse>>

}
