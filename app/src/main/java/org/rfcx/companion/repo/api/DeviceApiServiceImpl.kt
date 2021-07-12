package org.rfcx.companion.repo.api

import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.repo.ApiManager
import retrofit2.Call

//TODO: Convert Old device api to MVVM pattern

class DeviceApiServiceImpl: DeviceApiService {
    override fun getProjects(
        authUser: String,
        limit: Int,
        offset: Int
    ): Call<List<ProjectResponse>> {
        return ApiManager.getInstance().getDeviceApi2().getProjects(authUser, limit, offset)
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

}