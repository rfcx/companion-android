package org.rfcx.audiomoth.localdb

import io.realm.Realm
import org.rfcx.audiomoth.entity.Profile
import org.rfcx.audiomoth.entity.SyncState

class ProfileDb(private val realm: Realm) {

    fun getProfiles(): List<Profile> {
        return realm.where(Profile::class.java).findAll() ?: arrayListOf()
    }

    fun markUploading(id: Int) {
        mark(id = id, syncState = SyncState.Uploading.key)
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

    fun insertOrUpdateProfile(profile: Profile){
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