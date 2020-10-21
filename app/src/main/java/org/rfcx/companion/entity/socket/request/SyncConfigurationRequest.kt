package org.rfcx.companion.entity.socket.request

data class SyncConfigurationRequest(
    val command: SyncConfiguration
)

data class SyncConfiguration(
    val sync: List<String>
)
