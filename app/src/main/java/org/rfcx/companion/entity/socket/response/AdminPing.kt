package org.rfcx.companion.entity.socket.response

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class AdminPing(
    val network: String? = null,
    @SerializedName("sentinel_power")
    val sentinelPower: String? = null,
    @SerializedName("sentinel_sensor")
    val sentinelSensor: String? = null,
    val cpu: String? = null,
    val storage: String? = null,
    val companion: JsonObject? = null
) {
    fun toJson(): JsonObject {
        val gson = Gson()
        return gson.fromJson(gson.toJson(this), JsonObject::class.java)
    }
}
