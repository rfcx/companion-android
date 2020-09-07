package org.rfcx.audiomoth.entity.socket.response

import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration

data class ConfigurationResponse(
    val configure: GuardianConfiguration = GuardianConfiguration()
) : SocketResposne
