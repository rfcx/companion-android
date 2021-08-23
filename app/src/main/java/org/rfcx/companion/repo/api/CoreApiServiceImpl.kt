package org.rfcx.companion.repo.api

import org.rfcx.companion.entity.response.GuardianSoftwareResponse
import org.rfcx.companion.repo.ApiManager
import retrofit2.Call

class CoreApiServiceImpl: CoreApiService {
    override fun checkSoftwareVersion(authUser: String): Call<List<GuardianSoftwareResponse>> {
        return ApiManager.getInstance().coreApi.checkSoftwareVersion(authUser)
    }

}
