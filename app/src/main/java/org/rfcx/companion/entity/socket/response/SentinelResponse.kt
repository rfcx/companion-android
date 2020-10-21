package org.rfcx.companion.entity.socket.response

data class SentinelResponse(
    val sentinel: SentinelInfo = SentinelInfo()
) : SocketResposne

data class SentinelInfo(
    val input: SentinelInput = SentinelInput(),
    val system: SentinelSystem = SentinelSystem(),
    val battery: SentinelBattery = SentinelBattery()
)

data class SentinelInput(
    val current: Int = 0,
    val voltage: Int = 0,
    val power: Int = 0
)

data class SentinelSystem(
    val current: Int = 0,
    val voltage: Int = 0,
    val power: Int = 0
)

data class SentinelBattery(
    val current: Int = 0,
    val voltage: Int = 0,
    val power: Int = 0
)
