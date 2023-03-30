package org.rfcx.companion.repo

import org.rfcx.companion.entity.response.FirebaseAuthResponse
import retrofit2.Call
import retrofit2.http.GET

interface FirebaseAuthInterface {
    @GET("firebaseToken")
    fun firebaseAuth(): Call<FirebaseAuthResponse>
}
