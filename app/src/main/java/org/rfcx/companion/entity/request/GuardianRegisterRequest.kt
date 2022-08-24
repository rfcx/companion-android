package org.rfcx.companion.entity.request

data class GuardianRegisterRequest(
    val guid: String,
    val token: String?,
    val pinCode: String?
)
