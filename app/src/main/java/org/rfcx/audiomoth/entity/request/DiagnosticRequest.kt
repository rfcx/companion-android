package org.rfcx.audiomoth.entity.request

import java.util.*
import org.rfcx.audiomoth.entity.guardian.DiagnosticInfo

data class DiagnosticRequest(
    val deploymentServerId: String? = null,
    var lastConnection: Date = Date()
)

fun DiagnosticInfo.toRequestBody(): DiagnosticRequest {
    return DiagnosticRequest(
        deploymentServerId = this.deploymentServerId,
        lastConnection = this.lastConnection
    )
}
