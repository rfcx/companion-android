package org.rfcx.companion.entity.request

data class GuardianDeploymentParameters(
    val guid: String?,
    val token: String?,
    val keystorePassphrase: String?,
    val pin_code: String?,
    val apiMqttHost: String?,
    val apiSmsAddress: String?
)
