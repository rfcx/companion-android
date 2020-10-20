package org.rfcx.companion.entity.socket.response

import com.google.gson.annotations.SerializedName

data class CheckGuardianRegistered(
    @SerializedName("is_registered")
    val isRegistered: Boolean = false
)
