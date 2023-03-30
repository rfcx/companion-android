package org.rfcx.companion.repo.api

import android.content.Context
import okhttp3.ResponseBody
import org.rfcx.companion.entity.response.GuardianSoftwareResponse
import org.rfcx.companion.repo.ApiManager
import retrofit2.Call

class CoreApiServiceImpl(private val context: Context) : CoreApiService {
    override fun checkSoftwareVersion(): Call<List<GuardianSoftwareResponse>> {
        return ApiManager.getInstance().getCoreApi(context).checkSoftwareVersion()
    }

    override fun downloadFile(url: String): Call<ResponseBody> {
        return ApiManager.getInstance().getCoreApi(context).downloadFile(url)
    }
}
