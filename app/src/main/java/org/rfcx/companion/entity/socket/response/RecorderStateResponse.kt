package org.rfcx.companion.entity.socket.response

import com.google.gson.annotations.SerializedName

data class RecorderStateResponse(
    @SerializedName("is_recording")
    val isRecording: Boolean = false
)
