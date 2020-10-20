package org.rfcx.companion.repo

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiManager {
    var apiRest: ApiRestInterface
    var apiFirebaseAuth: FirebaseAuthInterface

    companion object {
        @Volatile
        private var INSTANCE: ApiManager? = null
        const val DEPLOY_DOMAIN = "https://api.rfcx.org/"
        const val FIREBASE_AUTH_DOMAIN = "https://us-central1-rfcx-deployment.cloudfunctions.net/auth/"

        fun getInstance(): ApiManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiManager()
            }
    }

    init {
        apiRest = setRetrofitBaseUrl(DEPLOY_DOMAIN).create(ApiRestInterface::class.java)
        apiFirebaseAuth = setRetrofitBaseUrl(FIREBASE_AUTH_DOMAIN).create(FirebaseAuthInterface::class.java)
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
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
            }
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }
}
