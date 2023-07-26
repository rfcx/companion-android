package org.rfcx.companion.repo.api

import android.content.Context
import okhttp3.ResponseBody
import org.rfcx.companion.repo.ApiManager
import retrofit2.Call

class CoreApiServiceImpl(private val context: Context) : CoreApiService {

    override fun downloadFile(url: String): Call<ResponseBody> {
        return ApiManager.getInstance().getCoreApi(context).downloadFile(url)
    }
}
