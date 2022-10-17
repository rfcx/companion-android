package org.rfcx.companion.localdb

import io.realm.Realm
import io.realm.RealmResults
import org.rfcx.companion.entity.guardian.GuardianRegistration

class GuardianRegistrationDb(private val realm: Realm) {

    fun count(): Int {
        return realm.where(GuardianRegistration::class.java)
            .count().toInt()
    }

    fun getAll(): List<GuardianRegistration> {
        return realm.where(GuardianRegistration::class.java)
            .findAll()
    }

    fun getAllForWorker(): List<GuardianRegistration> {
        var unsent: List<GuardianRegistration> = listOf()
        realm.executeTransaction {
            val registrations = realm.where(GuardianRegistration::class.java)
                .findAll().createSnapshot()
            unsent = registrations
        }
        return unsent
    }

    fun getAllResultsAsync(): RealmResults<GuardianRegistration> {
        return realm.where(GuardianRegistration::class.java)
            .findAllAsync()
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
