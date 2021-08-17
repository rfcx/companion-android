package org.rfcx.companion.entity.response

import com.google.gson.annotations.SerializedName

data class GuardianRegisterResponse(
    val guid: String,
    val token: String,
    @SerializedName("keystore_passphrase")
    val keystorePassPhase: String,
    @SerializedName("pin_code")
    val pinCode: String?,
    @SerializedName("api_mqtt_host")
    val apiMqttHost: String?,
    @SerializedName("api_sms_address")
    val apiSmsAddress: String?
)
