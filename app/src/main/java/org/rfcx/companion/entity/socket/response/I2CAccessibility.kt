package org.rfcx.companion.entity.socket.response

import com.google.gson.annotations.SerializedName

data class I2CAccessibility(
    @SerializedName("is_accessible")
    val isAccessible: Boolean = false,
    val message: String? = null
)
