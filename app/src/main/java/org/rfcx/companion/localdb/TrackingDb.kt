package org.rfcx.companion.localdb

import io.realm.Realm
import org.rfcx.companion.entity.Tracking

class TrackingDb(private val realm: Realm) {
    fun insertOrUpdate(tracking: Tracking) {
        realm.executeTransaction {
            if (tracking.id == 0) {
                val id =
                    (realm.where(Tracking::class.java).max(Tracking.TRACKING_ID)
                        ?.toInt() ?: 0) + 1
                tracking.id = id
            }
            it.insertOrUpdate(tracking)
        }
    }

    fun getTracking(): List<Tracking> {
        return realm.where(Tracking::class.java).findAll() ?: arrayListOf()
    }

    fun getCountTracking(): Int {
        return realm.where(Tracking::class.java).findAll().size
    }
}
