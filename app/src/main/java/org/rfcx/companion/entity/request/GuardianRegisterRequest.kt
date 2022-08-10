package org.rfcx.companion.entity.request

import com.google.gson.annotations.SerializedName

data class GuardianRegisterRequest(
    val guid: String,
    val token: String?,
    @SerializedName("pin_code")
    val pinCode: String?
)
