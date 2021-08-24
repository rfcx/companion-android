package org.rfcx.companion.repo.api

import okhttp3.ResponseBody
import org.rfcx.companion.entity.response.GuardianSoftwareResponse
import org.rfcx.companion.repo.ApiManager
import retrofit2.Call

class CoreApiServiceImpl: CoreApiService {
    override fun checkSoftwareVersion(authUser: String): Call<List<GuardianSoftwareResponse>> {
        return ApiManager.getInstance().coreApi.checkSoftwareVersion(authUser)
    }

    override fun downloadAPK(url: String): Call<ResponseBody> {
        return ApiManager.getInstance().coreApi.downloadAPK(url)
    }

}
