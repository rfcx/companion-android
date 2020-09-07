package org.rfcx.audiomoth.entity.socket.response

import com.google.gson.JsonArray
import org.rfcx.audiomoth.entity.guardian.Diagnostic
import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration

data class DiagnosticResponse(
    val diagnostic: Diagnostic = Diagnostic(),
    val configure: GuardianConfiguration = GuardianConfiguration(),
    val prefs: JsonArray = JsonArray()
) : SocketResposne
