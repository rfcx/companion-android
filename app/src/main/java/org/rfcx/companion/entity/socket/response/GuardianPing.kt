package org.rfcx.companion.entity.socket.response

import com.google.gson.JsonObject

data class GuardianPing(
    val prefs: JsonObject? = null,
    val device: JsonObject? = null,
    val software: String? = null,
    val instructions: JsonObject? = null,
    val companion: JsonObject? = null,
    val swm: String? = null,
    val battery: String? = null
)
