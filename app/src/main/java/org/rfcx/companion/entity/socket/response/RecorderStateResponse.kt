package org.rfcx.companion.entity.socket.response

data class RecorderStateResponse(
    val recorder: Recorder = Recorder()
)

data class Recorder(
    val isRecording: Boolean = false
)
