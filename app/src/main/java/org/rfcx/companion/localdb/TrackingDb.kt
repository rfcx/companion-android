package org.rfcx.companion.localdb

import io.realm.Realm
import org.rfcx.companion.entity.Coordinate
import org.rfcx.companion.entity.Tracking
import java.util.*

class TrackingDb(private val realm: Realm) {
    fun insertOrUpdate(tracking: Tracking, coordinate: Coordinate) {
        realm.executeTransaction {
            val track =
                it.where(Tracking::class.java).equalTo(Tracking.TRACKING_ID, tracking.id)
                    .findFirst()

            if (track != null) {
                tracking.startAt = track.startAt
                tracking.points = track.points
            }

            tracking.stopAt = Date()
            tracking.points.add(coordinate)
            it.insertOrUpdate(tracking)
        }
    }

    fun deleteTracking(id: Int) {
        realm.executeTransaction {
            val tracking =
                it.where(Tracking::class.java).equalTo(Tracking.TRACKING_ID, id)
                    .findFirst()
            tracking?.deleteFromRealm()
        }
    }

    fun getTracking(): List<Tracking> {
        return realm.where(Tracking::class.java).findAll() ?: arrayListOf()
    }

    fun getCountTracking(): Int {
        return realm.where(Tracking::class.java).findAll().size
    }
}
