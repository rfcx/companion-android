package org.rfcx.companion.util

import com.google.gson.JsonObject

fun Map<String, String>.toJsonObject(): JsonObject {
    val jsonObject = JsonObject()
    this.forEach {
        jsonObject.addProperty(it.key, it.value)
    }
    return jsonObject
}
