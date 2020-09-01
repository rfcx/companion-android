package org.rfcx.audiomoth.entity.socket

import com.google.gson.annotations.SerializedName

data class SentinelResponse(
    val sentinel: SentinelInfo = SentinelInfo()
) : SocketResposne

data class SentinelInfo(
    @SerializedName("is_solar_attached")
    val isSolarAttached: Boolean = false,
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
