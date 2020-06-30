package org.rfcx.audiomoth.localdb.guardian

import io.realm.Realm
import org.rfcx.audiomoth.entity.SyncState
import org.rfcx.audiomoth.entity.guardian.GuardianProfile

class GuardianProfileDb(private val realm: Realm) {

    fun unsentCount(): Long {
        return realm.where(GuardianProfile::class.java)
            .notEqualTo(GuardianProfile.FIELD_SYNC_STATE, SyncState.Sent.key)
            .count()
    }

    fun unlockSending() {
        realm.executeTransaction {
            val snapshot = it.where(GuardianProfile::class.java).equalTo(GuardianProfile.FIELD_SYNC_STATE, SyncState.Sending.key).findAll().createSnapshot()
            snapshot.forEach { profile ->
                profile.syncState = SyncState.Unsent.key
            }
        }
    }

    fun lockUnsent(): List<GuardianProfile> {
        var unsentCopied: List<GuardianProfile> = listOf()
        realm.executeTransaction {
            val unsent = it.where(GuardianProfile::class.java)
                .equalTo(GuardianProfile.FIELD_SYNC_STATE, SyncState.Unsent.key).findAll()
                .createSnapshot()
            unsentCopied = unsent.toList()
            unsent.forEach {d->
                d.syncState = SyncState.Sending.key
            }
        }
        return unsentCopied
    }

    fun getProfiles(): List<GuardianProfile> {
        return realm.where(GuardianProfile::class.java).findAll() ?: arrayListOf()
    }

    fun markUploading(id: Int) {
        mark(id = id, syncState = SyncState.Sending.key)
    }

    fun markUnsent(id: Int) {
        mark(id = id, syncState = SyncState.Unsent.key)
    }

    fun markSent(serverId: String, id: Int) {
        mark(id, serverId, SyncState.Sent.key)
    }

    private fun mark(id: Int, serverId: String? = null, syncState: Int) {
        realm.executeTransaction {
            val profile = it.where(GuardianProfile::class.java).equalTo(GuardianProfile.FIELD_ID, id).findFirst()
            if (profile != null) {
                profile.serverId = serverId
                profile.syncState = syncState
            }
        }
    }

    fun insertOrUpdateProfile(profile: GuardianProfile){
        realm.executeTransaction {
            if (profile.id == 0) {
                val id = (realm.where(GuardianProfile::class.java).max(GuardianProfile.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                profile.id = id
            }
            it.insertOrUpdate(profile)
        }
    }
}
