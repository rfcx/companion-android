package org.rfcx.companion.repo

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.rfcx.companion.BuildConfig
import org.rfcx.companion.repo.api.CoreApiService
import org.rfcx.companion.repo.api.DeviceApiService
import org.rfcx.companion.repo.api.TokenAuthenticator
import org.rfcx.companion.util.insert
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiManager {
    private lateinit var coreApi: CoreApiService
    private lateinit var apiFirebaseAuth: FirebaseAuthInterface
    private lateinit var deviceApi: DeviceApiInterface
    private lateinit var deviceApi2: DeviceApiService

    companion object {
        @Volatile
        private var INSTANCE: ApiManager? = null

        fun getInstance(): ApiManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiManager()
            }
    }

    fun getCoreApi(context: Context): CoreApiService {
        coreApi = setRetrofitBaseUrl(BuildConfig.DEPLOY_DOMAIN, context).create(CoreApiService::class.java)
        return coreApi
    }

    fun getApiFirebaseAuth(context: Context): FirebaseAuthInterface {
        apiFirebaseAuth = setRetrofitBaseUrl(BuildConfig.FIREBASE_AUTH_DOMAIN, context).create(FirebaseAuthInterface::class.java)
        return apiFirebaseAuth

    }

    fun getDeviceApi(context: Context): DeviceApiInterface {
        deviceApi = setRetrofitBaseUrl(BuildConfig.DEVICE_API_DOMAIN, context).create(DeviceApiInterface::class.java)
        return deviceApi
    }

    fun getDeviceApi2(context: Context): DeviceApiService {
        deviceApi2 = setRetrofitBaseUrl(BuildConfig.DEVICE_API_DOMAIN, context).create(DeviceApiService::class.java)
        return deviceApi2
    }

    fun getDeviceApi2(isProduction: Boolean? = null, context: Context): DeviceApiService {
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
            setRetrofitBaseUrl(url, context).create(DeviceApiService::class.java)
        }
    }

    private fun setRetrofitBaseUrl(baseUrl: String, context: Context): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseUrl)
            .client(createClient(context))
            .build()
    }

    private fun createClient(context: Context): OkHttpClient {
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
            .authenticator(TokenAuthenticator(context))
            .build()
    }
}
