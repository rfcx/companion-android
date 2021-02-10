package org.rfcx.companion.repo

import org.rfcx.companion.entity.response.FirebaseAuthResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

interface FirebaseAuthInterface {
    @GET("firebaseToken")
    fun firebaseAuth(@Header("Authorization") authUser: String): Call<FirebaseAuthResponse>

}
