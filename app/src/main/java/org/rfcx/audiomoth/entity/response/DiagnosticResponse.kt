package org.rfcx.audiomoth.entity.response

import java.util.*
import org.rfcx.audiomoth.entity.SyncState
import org.rfcx.audiomoth.entity.guardian.DiagnosticInfo

/**
 * Firestore response for getting a location
 */
data class DiagnosticResponse(
    var serverId: String? = null,
    var deploymentServerId: String? = null,
    var lastConnection: Date = Date()
) {
    fun toDiagnostic(): DiagnosticInfo {
        return DiagnosticInfo(
            serverId = this.serverId,
            deploymentServerId = this.deploymentServerId,
            lastConnection = this.lastConnection,
            syncState = SyncState.Sent.key
        )
    }
}
