package org.rfcx.companion.entity

data class DeviceParameter(
    val guid: String?,
    val guardianToken: String?,
    val ping: String?
)

data class SongMeterParameters(
    val songMeterPrefixes: String?
)
