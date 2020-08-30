package org.rfcx.audiomoth.entity.socket

data class SentinelResponse(
    val sentinel: SentinelInfo = SentinelInfo()
) : SocketResposne

data class SentinelInfo(
    val isSolarAttached: Boolean = false,
    val voltage: Int = 0,
    val current: Int = 0,
    val power: Int = 0
)
