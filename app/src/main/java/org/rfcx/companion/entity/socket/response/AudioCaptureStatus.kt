package org.rfcx.companion.entity.socket.response

import com.google.gson.annotations.SerializedName

data class AudioCaptureStatus(
    @SerializedName("is_audio_capturing")
    val isCapturing: Boolean,
    @SerializedName("audio_capturing_message")
    val msg: String?
)
