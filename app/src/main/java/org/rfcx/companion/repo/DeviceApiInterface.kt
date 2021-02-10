package org.rfcx.companion.repo

import okhttp3.ResponseBody
import org.rfcx.companion.entity.request.DeploymentRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DeviceApiInterface {
    @POST("deployments")
    fun createDeployment(@Header("Authorization") authUser: String,
                         @Body deploymentRequest: DeploymentRequest): Call<ResponseBody>
}
