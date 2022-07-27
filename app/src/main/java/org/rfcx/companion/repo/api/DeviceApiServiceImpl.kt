package org.rfcx.companion.repo.api

import okhttp3.ResponseBody
import org.rfcx.companion.entity.UserTouchResponse
import org.rfcx.companion.entity.request.GuardianRegisterRequest
import org.rfcx.companion.entity.response.*
import org.rfcx.companion.repo.ApiManager
import retrofit2.Call

// TODO: Convert Old device api to MVVM pattern

class DeviceApiServiceImpl : DeviceApiService {
    override fun getProjects(
        authUser: String,
        limit: Int,
        offset: Int,
        fields: List<String>
    ): Call<List<ProjectResponse>> {
        return ApiManager.getInstance().getDeviceApi2().getProjects(authUser, limit, offset, fields)
    }

    override fun getDeletedProjects(
        authUser: String,
        limit: Int,
        offset: Int,
        onlyDeleted: Boolean,
        fields: List<String>
    ): Call<List<ProjectResponse>> {
        return ApiManager.getInstance().getDeviceApi2().getDeletedProjects(authUser, limit, offset, onlyDeleted, fields)
    }

    override fun getStreamAssets(
        authUser: String,
        id: String
    ): Call<List<DeploymentAssetResponse>> {
        return ApiManager.getInstance().getDeviceApi2().getStreamAssets(authUser, id)
    }

    override fun userTouch(authUser: String): Call<UserTouchResponse> {
        return ApiManager.getInstance().getDeviceApi2().userTouch(authUser)
    }

    override fun getProjectsById(authUser: String, id: String): Call<ProjectByIdResponse> {
        return ApiManager.getInstance().getDeviceApi2().getProjectsById(authUser, id)
    }

    override fun registerGuardian(
        authUser: String,
        guid: GuardianRegisterRequest
    ): Call<GuardianRegisterResponse> {
        return ApiManager.getInstance().getDeviceApi2().registerGuardian(authUser, guid)
    }

    override fun checkAvailableClassifiers(authUser: String): Call<List<GuardianClassifierResponse>> {
        return ApiManager.getInstance().getDeviceApi2().checkAvailableClassifiers(authUser)
    }

    override fun downloadFile(url: String): Call<ResponseBody> {
        return ApiManager.getInstance().getDeviceApi2().downloadFile(url)
    }
}
