package org.rfcx.audiomoth.entity.socket

import com.google.gson.annotations.SerializedName

data class SentinelResponse(
    val sentinel: SentinelInfo = SentinelInfo()
) : SocketResposne

data class SentinelInfo(
    @SerializedName("is_solar_attached")
    val isSolarAttached: Boolean = false,
    val voltage: Int = 0,
    val current: Int = 0,
    val power: Int = 0
)
