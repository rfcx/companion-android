package org.rfcx.companion.repo.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import org.rfcx.companion.util.getIdToken

class TokenInterceptor(private val context: Context): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${context.getIdToken()}")
            .build()
        return chain.proceed(newRequest)
    }
}
