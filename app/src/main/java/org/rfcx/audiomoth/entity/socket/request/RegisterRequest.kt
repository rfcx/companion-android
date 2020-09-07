package org.rfcx.audiomoth.entity.socket.request

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val command: Register
)

data class Register(
    @SerializedName("token_id")
    val tokenId: String
)
