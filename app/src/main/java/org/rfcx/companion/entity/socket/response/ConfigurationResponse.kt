package org.rfcx.companion.entity.socket.response

import org.rfcx.companion.entity.guardian.GuardianConfiguration

data class ConfigurationResponse(
    val configure: GuardianConfiguration = GuardianConfiguration()
) : SocketResposne
