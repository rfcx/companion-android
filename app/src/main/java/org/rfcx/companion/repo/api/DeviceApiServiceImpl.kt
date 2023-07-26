package org.rfcx.companion.repo.api

import android.content.Context
import okhttp3.ResponseBody
import org.rfcx.companion.entity.UserTouchResponse
import org.rfcx.companion.entity.response.*
import org.rfcx.companion.repo.ApiManager
import retrofit2.Call

// TODO: Convert Old device api to MVVM pattern

class DeviceApiServiceImpl(private val context: Context) : DeviceApiService {
    override fun getProjects(
        limit: Int,
        offset: Int,
        fields: List<String>
    ): Call<List<ProjectResponse>> {
        return ApiManager.getInstance().getDeviceApi2(context).getProjects(limit, offset, fields)
    }

    override fun getDeletedProjects(
        limit: Int,
        offset: Int,
        onlyDeleted: Boolean,
        fields: List<String>
    ): Call<List<ProjectResponse>> {
        return ApiManager.getInstance().getDeviceApi2(context).getDeletedProjects(limit, offset, onlyDeleted, fields)
    }

    override fun getProjectOffTime(
        id: String
    ): Call<ProjectOffTimeResponse> {
        return ApiManager.getInstance().getDeviceApi2(context).getProjectOffTime(id)
    }

    override fun getStreamAssets(
        id: String
    ): Call<List<DeploymentAssetResponse>> {
        return ApiManager.getInstance().getDeviceApi2(context).getStreamAssets(id)
    }

    override fun userTouch(): Call<UserTouchResponse> {
        return ApiManager.getInstance().getDeviceApi2(context).userTouch()
    }

    override fun getProjectsById(id: String): Call<ProjectByIdResponse> {
        return ApiManager.getInstance().getDeviceApi2(context).getProjectsById(id)
    }

    override fun downloadFile(url: String): Call<ResponseBody> {
        return ApiManager.getInstance().getDeviceApi2(context).downloadFile(url)
    }
}
