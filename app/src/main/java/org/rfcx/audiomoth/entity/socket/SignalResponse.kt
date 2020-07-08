package org.rfcx.audiomoth.entity.socket

data class SignalResponse(
    val signal: Signal
) : SocketResposne

data class Signal(
    val strength: Int
)
