package org.rfcx.audiomoth.entity.socket

import com.google.gson.JsonArray

data class PrefsResponse(
    val prefs: JsonArray
) : SocketResposne
