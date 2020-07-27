package org.rfcx.audiomoth.localdb

import io.realm.Realm
import org.rfcx.audiomoth.entity.Profile
import org.rfcx.audiomoth.entity.SyncState

class ProfileDb(private val realm: Realm) {

    fun unsentCount(): Long {
        return realm.where(Profile::class.java)
            .notEqualTo(Profile.FIELD_SYNC_STATE, SyncState.Sent.key)
            .count()
    }

    fun unlockSending() {
        realm.executeTransaction {
            val snapshot = it.where(Profile::class.java)
                .equalTo(Profile.FIELD_SYNC_STATE, SyncState.Sending.key).findAll().createSnapshot()
            snapshot.forEach {
                it.syncState = SyncState.Unsent.key
            }
        }
    }

    fun isExistingProfile(name: String): Boolean {
        val count = realm.where(Profile::class.java)
            .equalTo(Profile.FIELD_NAME, name).count()
        return count.toInt() != 0
    }

    fun getProfiles(): List<Profile> {
        return realm.where(Profile::class.java).findAll() ?: arrayListOf()
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
            val profile = it.where(Profile::class.java).equalTo(Profile.FIELD_ID, id).findFirst()
            if (profile != null) {
                profile.serverId = serverId
                profile.syncState = syncState
            }
        }
    }

    fun lockUnsent(): List<Profile> {
        var unsentCopied: List<Profile> = listOf()
        realm.executeTransaction {
            val unsent = it.where(Profile::class.java)
                .equalTo(Profile.FIELD_SYNC_STATE, SyncState.Unsent.key).findAll()
                .createSnapshot()
            unsentCopied = unsent.toList()
            unsent.forEach { d ->
                d.syncState = SyncState.Sending.key
            }
        }
        return unsentCopied
    }

    fun insertOrUpdateProfile(profile: Profile) {
        realm.executeTransaction {
            if (profile.id == 0) {
                val id = (realm.where(Profile::class.java).max(Profile.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                profile.id = id
            }
            it.insertOrUpdate(profile)
        }
    }
}
