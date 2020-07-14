package org.rfcx.audiomoth.localdb.guardian

import io.realm.Realm
import org.rfcx.audiomoth.entity.SyncState
import org.rfcx.audiomoth.entity.guardian.Diagnostic
import org.rfcx.audiomoth.entity.guardian.DiagnosticInfo
import org.rfcx.audiomoth.entity.response.DiagnosticResponse
import java.util.*

class DiagnosticDb(private val realm: Realm) {

    fun unsentCount(): Long {
        return realm.where(Diagnostic::class.java)
            .notEqualTo(DiagnosticInfo.FIELD_SYNC_STATE, SyncState.Sent.key)
            .count()
    }

    fun getDiagnosticInfo(deploymentServerId: String?): DiagnosticInfo {
        var diagnosticInfo: DiagnosticInfo? = null
        realm.executeTransaction {
            diagnosticInfo = it.where(DiagnosticInfo::class.java)
                .equalTo(DiagnosticInfo.FIELD_DEPLOY_ID, deploymentServerId)
                .findFirst()
        }
        return diagnosticInfo ?: DiagnosticInfo()
    }

    private fun getIdByDeploymentServerId(deploymentServerId: String?): Int {
        var id = 0
        realm.executeTransaction {
            id = it.where(DiagnosticInfo::class.java)
                .equalTo(DiagnosticInfo.FIELD_DEPLOY_ID, deploymentServerId)
                .max(DiagnosticInfo.FIELD_ID)?.toInt() ?: 0
        }
        return id
    }

    fun insertOrUpdate(diagnostic: Diagnostic, deploymentServerId: String?) {
        var id = getIdByDeploymentServerId(deploymentServerId)
        realm.executeTransaction {
            if (id == 0) {
                val diagnosticInfo =
                    DiagnosticInfo(deploymentServerId = deploymentServerId, diagnostic = diagnostic)
                id = (it.where(DiagnosticInfo::class.java).max(DiagnosticInfo.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                diagnosticInfo.id = id
                diagnosticInfo.syncState = SyncState.Unsent.key
                it.insertOrUpdate(diagnosticInfo)

            } else {
                val diagnosticInfo =
                    it.where(DiagnosticInfo::class.java)
                        .equalTo(DiagnosticInfo.FIELD_ID, id)
                        .findFirst()
                if (diagnosticInfo != null) {
                    val newDiagnosticInfo = DiagnosticInfo(
                        diagnosticInfo.id,
                        diagnosticInfo.serverId,
                        diagnosticInfo.deploymentServerId,
                        diagnostic,
                        Date(),
                        SyncState.Unsent.key
                    )
                    it.insertOrUpdate(newDiagnosticInfo)
                }

            }
        }
    }

    fun markUnsent(id: Int) {
        mark(id = id, syncState = SyncState.Unsent.key)
    }

    fun markSent(serverId: String, id: Int) {
        mark(id, serverId, SyncState.Sent.key)
    }

    private fun mark(id: Int, serverId: String? = null, syncState: Int) {
        realm.executeTransaction {
            val diagnostic =
                it.where(DiagnosticInfo::class.java).equalTo(DiagnosticInfo.FIELD_ID, id)
                    .findFirst()
            if (diagnostic != null) {
                diagnostic.serverId = serverId
                diagnostic.syncState = syncState
            }
        }
    }

    fun unlockSent(): List<DiagnosticInfo> {
        var unsentCopied: List<DiagnosticInfo> = listOf()
        realm.executeTransaction {
            val unsent = it.where(DiagnosticInfo::class.java)
                .equalTo(DiagnosticInfo.FIELD_SYNC_STATE, SyncState.Unsent.key)
                .findAll()
                .createSnapshot()
            unsentCopied = unsent.toList()
            unsent.forEach { diagnostic ->
                diagnostic.syncState = SyncState.Sending.key
            }
        }
        return unsentCopied
    }

    fun insertOrUpdate(diagnosticResponse: DiagnosticResponse) {
        realm.executeTransaction {
            val diagnostic =
                it.where(DiagnosticInfo::class.java)
                    .equalTo(DiagnosticInfo.FIELD_SERVER_ID, diagnosticResponse.serverId)
                    .findFirst()

            if (diagnostic != null) {
                diagnostic.serverId = diagnosticResponse.serverId
                diagnostic.deploymentServerId = diagnosticResponse.deploymentServerId
                diagnostic.diagnostic = diagnostic.diagnostic
                diagnostic.createdAt = diagnosticResponse.createdAt
            } else {
                val diagnosticInfo = diagnosticResponse.toDiagnostic()
                val id = (it.where(DiagnosticInfo::class.java).max(DiagnosticInfo.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                diagnosticInfo.id = id
                it.insert(diagnosticInfo)
            }
        }
    }

}
