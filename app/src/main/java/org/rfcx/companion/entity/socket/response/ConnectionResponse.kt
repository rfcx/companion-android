package org.rfcx.companion.entity.socket.response

data class ConnectionResponse(
    val connection: Connection = Connection()
) : SocketResposne

data class Connection(
    val status: String = Status.FAILED.value
)

enum class Status(val value: String) { SUCCESS("success"), FAILED("failed") }
