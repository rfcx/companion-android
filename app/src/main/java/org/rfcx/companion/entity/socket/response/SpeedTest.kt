package org.rfcx.companion.entity.socket.response

data class SpeedTest(
    val downloadSpeed: Double,
    val uploadSpeed: Double,
    val isFailed: Boolean,
    val hasConnection: Boolean
)