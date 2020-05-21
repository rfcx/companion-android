package org.rfcx.audiomoth.repo

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiManager {
    var apiRest: ApiRestInterface

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
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(DEPLOY_DOMAIN)
            .client(createClient())
            .build()

        apiRest = retrofit.create(ApiRestInterface::class.java)
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