package org.rfcx.companion.entity.socket.response

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class AdminPing(
    val network: JsonObject? = null,
    @SerializedName("sentinel_power")
    val sentinelPower: JsonObject? = null,
    @SerializedName("sentinel_sensor")
    val sentinelSensor: JsonObject? = null,
    val cpu: JsonObject? = null,
    val storage: JsonObject? = null
)
