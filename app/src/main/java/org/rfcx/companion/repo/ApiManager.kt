package org.rfcx.companion.repo

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.rfcx.companion.BuildConfig
import org.rfcx.companion.repo.api.DeviceApiService
import org.rfcx.companion.util.insert
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiManager {
    var apiRest: ApiRestInterface
    var apiFirebaseAuth: FirebaseAuthInterface
    private var deviceApi: DeviceApiInterface
    private var deviceApi2: DeviceApiService

    companion object {
        @Volatile
        private var INSTANCE: ApiManager? = null

        fun getInstance(): ApiManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiManager()
            }
    }

    init {
        apiRest = setRetrofitBaseUrl(BuildConfig.DEPLOY_DOMAIN).create(ApiRestInterface::class.java)
        apiFirebaseAuth =
            setRetrofitBaseUrl(BuildConfig.FIREBASE_AUTH_DOMAIN).create(FirebaseAuthInterface::class.java)
        deviceApi =
            setRetrofitBaseUrl(BuildConfig.DEVICE_API_DOMAIN).create(DeviceApiInterface::class.java)
        deviceApi2 =
            setRetrofitBaseUrl(BuildConfig.DEVICE_API_DOMAIN).create(DeviceApiService::class.java)
    }

    fun getDeviceApi(): DeviceApiInterface = deviceApi

    fun getRestApi(isProduction: Boolean? = null): ApiRestInterface {
        return if (isProduction == null) {
            apiRest
        } else {
            val staging = "staging-"
            var url = BuildConfig.DEPLOY_DOMAIN
            if (url.contains(staging, ignoreCase = true)) {
                url = url.replace(staging, "")
            }
            if (!isProduction) {
                url = url.insert(8, staging)
            }
            setRetrofitBaseUrl(url).create(ApiRestInterface::class.java)
        }
    }
    fun getDeviceApi2(): DeviceApiService = deviceApi2

    private fun setRetrofitBaseUrl(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseUrl)
            .client(createClient())
            .build()
    }

    private fun createClient(): OkHttpClient {
        // okHttp log
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        return OkHttpClient.Builder()
            .apply {
                readTimeout(60, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)
            }
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }
}
