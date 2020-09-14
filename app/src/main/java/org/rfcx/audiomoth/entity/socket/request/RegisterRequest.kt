package org.rfcx.audiomoth.entity.socket.request

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val command: Register
)

data class Register(
    val register: RegisterInfo
)

data class RegisterInfo(
    @SerializedName("token_id")
    val tokenId: String,
    @SerializedName("is_production")
    val isProduction: Boolean
)
