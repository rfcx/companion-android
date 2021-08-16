package org.rfcx.companion.entity.socket.response

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class Ping (
    val prefs: JsonObject? = null,
    val device: JsonObject? = null,
    val software: String? = null,
    val instructions: JsonObject? = null,
    val companion: JsonObject? = null
    )
