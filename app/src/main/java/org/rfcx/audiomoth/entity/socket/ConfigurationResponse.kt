package org.rfcx.audiomoth.entity.socket

import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration

data class ConfigurationResponse(
    val configure: GuardianConfiguration
) : SocketResposne
