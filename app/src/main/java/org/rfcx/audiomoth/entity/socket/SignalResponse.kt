package org.rfcx.audiomoth.entity.socket

import com.google.gson.annotations.SerializedName

data class SignalResponse(
    @SerializedName("signal_info")
    val signalInfo: Signal
) : SocketResposne

data class Signal(
    val signal: Int,
    @SerializedName("sim_card")
    val simCard: Boolean
)
