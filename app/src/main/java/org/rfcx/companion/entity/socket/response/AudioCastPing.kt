package org.rfcx.companion.entity.socket.response

import com.google.gson.annotations.SerializedName

data class AudioCastPing(
    val amount: Int = 0,
    val number: Int = 0,
    val buffer: String = "",
    @SerializedName("read_size")
    val readSize: Int = 0
)
