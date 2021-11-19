package org.rfcx.companion.entity.response

data class GuardianRegisterResponse(
    val guid: String,
    val token: String,
    val keystorePassphrase: String,
    val pinCode: String?,
    val apiMqttHost: String?,
    val apiSmsAddress: String?
)
