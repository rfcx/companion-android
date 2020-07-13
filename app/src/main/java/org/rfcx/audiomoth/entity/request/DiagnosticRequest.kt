package org.rfcx.audiomoth.entity.request

import org.rfcx.audiomoth.entity.guardian.Diagnostic
import org.rfcx.audiomoth.entity.guardian.DiagnosticInfo
import java.util.*

data class DiagnosticRequest(
    val deploymentServerId: String? = null,
    val diagnostic: Diagnostic? = null,
    var createdAt: Date = Date()
)

fun DiagnosticInfo.toRequestBody(): DiagnosticRequest {
    return DiagnosticRequest(
        deploymentServerId = this.deploymentServerId,
        diagnostic = this.diagnostic,
        createdAt = this.createdAt
    )
}
