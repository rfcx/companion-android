package org.rfcx.audiomoth.entity.socket

import com.google.gson.annotations.SerializedName

data class MicrophoneTestResponse(
    @SerializedName("microphone_test")
    val audioBuffer: AudioBuffer
): SocketResposne

data class AudioBuffer(
    val buffer: String,
    @SerializedName("read_size")
    val readSize: Int
)
