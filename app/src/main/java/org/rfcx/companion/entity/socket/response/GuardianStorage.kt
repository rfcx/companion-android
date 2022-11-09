package org.rfcx.companion.entity.socket.response

data class GuardianStorage(
    val internal: Storage?,
    val external: Storage?
)

data class Storage(
    val used: Long,
    val all: Long
)
