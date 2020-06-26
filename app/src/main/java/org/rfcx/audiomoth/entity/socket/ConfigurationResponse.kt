package org.rfcx.audiomoth.entity.socket

import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration
import org.rfcx.audiomoth.entity.guardian.toReadableFormat

data class ConfigurationResponse(
    val configure: GuardianConfiguration
) : SocketResposne

fun ConfigurationResponse.toReadableFormat(): ConfigurationResponse {
    return ConfigurationResponse(configure.toReadableFormat())
}
