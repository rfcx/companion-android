package org.rfcx.companion.entity.socket.response

import com.google.gson.annotations.SerializedName

data class MicrophoneTestResponse(
    @SerializedName("microphone_test")
    val audioBuffer: AudioBuffer = AudioBuffer()
) : SocketResposne

data class AudioBuffer(
    val amount: Int = 0,
    val number: Int = 0,
    val buffer: String = "",
    @SerializedName("read_size")
    val readSize: Int = 0
)
