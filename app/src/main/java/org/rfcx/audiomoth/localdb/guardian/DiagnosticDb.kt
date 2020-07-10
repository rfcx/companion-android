package org.rfcx.audiomoth.localdb.guardian

import io.realm.Realm
import org.rfcx.audiomoth.entity.SyncState
import org.rfcx.audiomoth.entity.guardian.Diagnostic
import org.rfcx.audiomoth.entity.guardian.DiagnosticInfo

class DiagnosticDb(private val realm: Realm) {

    fun unsentCount(): Long {
        return realm.where(Diagnostic::class.java)
            .notEqualTo(DiagnosticInfo.FIELD_SYNC_STATE, SyncState.Sent.key)
            .count()
    }

    fun insertOrUpdate(diagnostic: DiagnosticInfo): Int {
        var id = diagnostic.id
        realm.executeTransaction {
            if (diagnostic.id == 0) {
                id = (it.where(DiagnosticInfo::class.java).max(DiagnosticInfo.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                diagnostic.id = id
            }
            it.insertOrUpdate(diagnostic)
        }
        return id
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
                it.where(DiagnosticInfo::class.java).equalTo(DiagnosticInfo.FIELD_ID, id).findFirst()
            if (diagnostic != null) {
                diagnostic.serverId = serverId
                diagnostic.syncState = syncState
            }
        }
    }

}
