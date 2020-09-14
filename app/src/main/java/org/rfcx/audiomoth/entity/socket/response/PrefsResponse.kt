package org.rfcx.audiomoth.entity.socket.response

import com.google.gson.JsonArray

data class PrefsResponse(
    val prefs: JsonArray = JsonArray()
) : SocketResposne
