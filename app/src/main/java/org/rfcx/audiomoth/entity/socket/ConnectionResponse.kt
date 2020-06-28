package org.rfcx.audiomoth.entity.socket

data class ConnectionResponse(
    val connection: Connection
) : SocketResposne

data class Connection(
    val status: String
)
