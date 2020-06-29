package org.rfcx.audiomoth.entity.socket

import com.google.gson.JsonArray
import com.google.gson.JsonObject

data class PrefsResponse(
    val prefs: JsonArray
) : SocketResposne
