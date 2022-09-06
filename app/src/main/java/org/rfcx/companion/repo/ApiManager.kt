package org.rfcx.companion.repo

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.rfcx.companion.BuildConfig
import org.rfcx.companion.repo.api.CoreApiService
import org.rfcx.companion.repo.api.DeviceApiService
import org.rfcx.companion.util.insert
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiManager {
    var coreApi: CoreApiService
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
        coreApi = setRetrofitBaseUrl(BuildConfig.DEPLOY_DOMAIN).create(CoreApiService::class.java)
        apiFirebaseAuth =
            setRetrofitBaseUrl(BuildConfig.FIREBASE_AUTH_DOMAIN).create(FirebaseAuthInterface::class.java)
        deviceApi =
            setRetrofitBaseUrl(BuildConfig.DEVICE_API_DOMAIN).create(DeviceApiInterface::class.java)
        deviceApi2 =
            setRetrofitBaseUrl(BuildConfig.DEVICE_API_DOMAIN).create(DeviceApiService::class.java)
    }

    fun getDeviceApi(): DeviceApiInterface = deviceApi

    fun getDeviceApi2(): DeviceApiService = deviceApi2

    fun getDeviceApi2(isProduction: Boolean? = null): DeviceApiService {
        return if (isProduction == null) {
            deviceApi2
        } else {
            val staging = "staging-"
            var url = BuildConfig.DEVICE_API_DOMAIN
            if (url.contains(staging, ignoreCase = true)) {
                url = url.replace(staging, "")
            }
            if (!isProduction) {
                url = url.insert(8, staging)
            }
            setRetrofitBaseUrl(url).create(DeviceApiService::class.java)
        }
    }

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
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().addHeader("User-Agent", "Companion/${BuildConfig.VERSION_NAME}/${BuildConfig.VERSION_CODE}").build()
                chain.proceed(request)
            }
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }
}
