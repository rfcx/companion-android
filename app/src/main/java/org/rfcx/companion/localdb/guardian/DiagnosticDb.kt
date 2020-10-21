package org.rfcx.companion.localdb.guardian

import io.realm.Realm
import java.util.*
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.entity.guardian.DiagnosticInfo
import org.rfcx.companion.entity.response.DiagnosticResponse

class DiagnosticDb(private val realm: Realm) {

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

    fun insertOrUpdate(deploymentServerId: String?) {
        var id = getIdByDeploymentServerId(deploymentServerId)
        realm.executeTransaction {
            if (id == 0) {
                val diagnosticInfo =
                    DiagnosticInfo(deploymentServerId = deploymentServerId)
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
                diagnostic.lastConnection = diagnosticResponse.lastConnection
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
