package org.rfcx.audiomoth.localdb

import io.realm.Realm
import org.rfcx.audiomoth.entity.Profile

class ProfileDb(private val realm: Realm) {

    fun getProfiles(): List<Profile> {
        return realm.where(Profile::class.java).findAll() ?: arrayListOf()
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