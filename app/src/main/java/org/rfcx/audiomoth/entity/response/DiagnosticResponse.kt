package org.rfcx.audiomoth.entity.response

import org.rfcx.audiomoth.entity.SyncState
import org.rfcx.audiomoth.entity.guardian.Diagnostic
import org.rfcx.audiomoth.entity.guardian.DiagnosticInfo
import java.util.*

/**
 * Firestore response for getting a location
 */
data class DiagnosticResponse(
    var serverId: String? = null,
    var deploymentServerId: String? = null,
    var diagnostic: Diagnostic? = null,
    var createdAt: Date = Date()
) {
    fun toDiagnostic(): DiagnosticInfo {
        return DiagnosticInfo(
            serverId = this.serverId,
            deploymentServerId = this.deploymentServerId,
            diagnostic = this.diagnostic,
            createdAt = this.createdAt,
            syncState = SyncState.Sent.key
        )
    }
}
