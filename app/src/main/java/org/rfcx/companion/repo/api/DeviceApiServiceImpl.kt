package org.rfcx.companion.repo.api

import android.content.Context
import okhttp3.ResponseBody
import org.rfcx.companion.entity.UserTouchResponse
import org.rfcx.companion.entity.request.GuardianRegisterRequest
import org.rfcx.companion.entity.response.*
import org.rfcx.companion.repo.ApiManager
import retrofit2.Call

// TODO: Convert Old device api to MVVM pattern

class DeviceApiServiceImpl(private val context: Context) : DeviceApiService {
    override fun getProjects(
        authUser: String,
        limit: Int,
        offset: Int,
        fields: List<String>
    ): Call<List<ProjectResponse>> {
        return ApiManager.getInstance().getDeviceApi2(context).getProjects(authUser, limit, offset, fields)
    }

    override fun getDeletedProjects(
        authUser: String,
        limit: Int,
        offset: Int,
        onlyDeleted: Boolean,
        fields: List<String>
    ): Call<List<ProjectResponse>> {
        return ApiManager.getInstance().getDeviceApi2(context).getDeletedProjects(authUser, limit, offset, onlyDeleted, fields)
    }

    override fun getProjectOffTime(
        authUser: String,
        id: String
    ): Call<ProjectOffTimeResponse> {
        return ApiManager.getInstance().getDeviceApi2(context).getProjectOffTime(authUser, id)
    }

    override fun getStreamAssets(
        authUser: String,
        id: String
    ): Call<List<DeploymentAssetResponse>> {
        return ApiManager.getInstance().getDeviceApi2(context).getStreamAssets(authUser, id)
    }

    override fun userTouch(authUser: String): Call<UserTouchResponse> {
        return ApiManager.getInstance().getDeviceApi2(context).userTouch(authUser)
    }

    override fun getProjectsById(authUser: String, id: String): Call<ProjectByIdResponse> {
        return ApiManager.getInstance().getDeviceApi2(context).getProjectsById(authUser, id)
    }

    override fun registerGuardian(
        authUser: String,
        guid: GuardianRegisterRequest
    ): Call<GuardianRegisterResponse> {
        return ApiManager.getInstance().getDeviceApi2(context).registerGuardian(authUser, guid)
    }

    override fun checkAvailableClassifiers(authUser: String): Call<List<GuardianClassifierResponse>> {
        return ApiManager.getInstance().getDeviceApi2(context).checkAvailableClassifiers(authUser)
    }

    override fun downloadFile(url: String): Call<ResponseBody> {
        return ApiManager.getInstance().getDeviceApi2(context).downloadFile(url)
    }
}
