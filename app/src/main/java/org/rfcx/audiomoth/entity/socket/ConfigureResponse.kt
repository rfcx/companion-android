package org.rfcx.audiomoth.entity.socket

data class ConfigureResponse(
    val configure: GuardianConfigure
)

data class GuardianConfigure(
    val file_format: String,
    val sample_rate: Int,
    val bitrate: Int,
    val duration: Int
)
