package org.rfcx.companion.repo

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.rfcx.companion.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiManager {
    var apiRest: ApiRestInterface
    var apiFirebaseAuth: FirebaseAuthInterface
    private var deviceApi: DeviceApiInterface

    companion object {
        @Volatile
        private var INSTANCE: ApiManager? = null
        const val DEPLOY_DOMAIN = "https://api.rfcx.org/"

        fun getInstance(): ApiManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiManager()
            }
    }

    init {
        apiRest = setRetrofitBaseUrl(DEPLOY_DOMAIN).create(ApiRestInterface::class.java)
        apiFirebaseAuth =
            setRetrofitBaseUrl(BuildConfig.FIREBASE_AUTH_DOMAIN).create(FirebaseAuthInterface::class.java)
        deviceApi =
            setRetrofitBaseUrl(BuildConfig.DEVICE_API_DOMAIN).create(DeviceApiInterface::class.java)
    }

    fun getDeviceApi(): DeviceApiInterface = deviceApi

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
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
            }
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }
}
