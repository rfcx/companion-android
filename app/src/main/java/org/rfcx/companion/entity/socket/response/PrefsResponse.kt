package org.rfcx.companion.entity.socket.response

import com.google.gson.JsonArray

data class PrefsResponse(
    val prefs: JsonArray = JsonArray()
) : SocketResposne
