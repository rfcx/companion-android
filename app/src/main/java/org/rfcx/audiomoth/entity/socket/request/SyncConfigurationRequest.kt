package org.rfcx.audiomoth.entity.socket.request

data class SyncConfigurationRequest(
    val command: SyncConfiguration
)

data class SyncConfiguration(
    val sync: List<String>
)
