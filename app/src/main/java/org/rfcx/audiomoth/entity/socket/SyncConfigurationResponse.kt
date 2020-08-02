package org.rfcx.audiomoth.entity.socket

data class SyncConfigurationResponse(
    val sync: Sync = Sync()
) : SocketResposne

data class Sync(
    val status: String = Status.FAILED.value
)
