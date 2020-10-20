package org.rfcx.companion.entity.socket.response

import com.google.gson.JsonArray
import org.rfcx.companion.entity.guardian.Diagnostic
import org.rfcx.companion.entity.guardian.GuardianConfiguration

data class DiagnosticResponse(
    val diagnostic: Diagnostic = Diagnostic(),
    val configure: GuardianConfiguration = GuardianConfiguration(),
    val prefs: JsonArray = JsonArray()
) : SocketResposne
