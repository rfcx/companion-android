package org.rfcx.companion.entity.socket.response

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class GuardianPing(
    val prefs: JsonObject? = null,
    val device: JsonObject? = null,
    val software: String? = null,
    val instructions: JsonObject? = null,
    val companion: JsonObject? = null,
    val swm: String? = null,
    val battery: String? = null,
    val library: JsonObject? = null,
    @SerializedName("active-classifier")
    val activeClassifier: JsonArray? = null,
) {
    fun toJson(): JsonObject {
        val gson = Gson()
        return gson.fromJson(gson.toJson(this), JsonObject::class.java)
    }
}
