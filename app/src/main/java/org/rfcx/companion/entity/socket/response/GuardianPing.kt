package org.rfcx.companion.entity.socket.response

import com.google.gson.JsonObject

data class GuardianPing(
    val prefs: Any? = null,
    val device: JsonObject? = null,
    val software: String? = null,
    val instructions: JsonObject? = null
)
