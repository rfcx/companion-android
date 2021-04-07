package org.rfcx.companion.entity.socket.request

data class CheckinRequest(
    val command: Checkin
)

data class Checkin(
    val checkin: CheckinInfo
)

data class CheckinInfo(
    val wantTo: String
)

enum class CheckinCommand(val value: String) {
    START("start"), STOP("stop")
}
