package org.rfcx.audiomoth.entity.socket.response

data class RegisterResponse(
    val register: RegisterStatus = RegisterStatus()
)

data class RegisterStatus(
    val status: String = Status.FAILED.value
)
