package org.rfcx.companion.localdb

import io.realm.Realm
import org.rfcx.companion.entity.guardian.GuardianRegistration

class GuardianRegistrationDb(private val realm: Realm) {

    fun count(): Int {
        return realm.where(GuardianRegistration::class.java)
            .count().toInt()
    }

    fun getAll(): List<GuardianRegistration>? {
        val registrations = realm.where(GuardianRegistration::class.java)
            .findAll()
        return realm.copyFromRealm(registrations)
    }

    fun insert(registration: GuardianRegistration) {
        realm.executeTransaction {
            it.insertOrUpdate(registration)
        }
    }

    fun delete(guid: String) {
        realm.executeTransaction {
            it.where(GuardianRegistration::class.java)
                .equalTo(GuardianRegistration.FIELD_GUID, guid)
                .findAll()
                .deleteAllFromRealm()
        }
    }
}
