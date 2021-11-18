package org.rfcx.companion.entity.socket.response

import com.google.gson.annotations.SerializedName

data class AdminPing(
    val network: String? = null,
    @SerializedName("sentinel_power")
    val sentinelPower: String? = null,
    @SerializedName("sentinel_sensor")
    val sentinelSensor: String? = null,
    val cpu: String? = null,
    val storage: String? = null,
    @SerializedName("swm_network")
    val swmNetwork: String? = null
)
