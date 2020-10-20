package org.rfcx.companion.entity.socket.response

data class SyncConfigurationResponse(
    val sync: Sync = Sync()
) : SocketResposne

data class Sync(
    val status: String = Status.FAILED.value
)
