package org.rfcx.companion.localdb

import android.content.Context
import io.realm.Realm
import org.rfcx.companion.entity.Coordinate
import org.rfcx.companion.entity.Tracking
import org.rfcx.companion.util.Preferences
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

    fun deleteTracking(id: Int, context: Context) {
        realm.executeTransaction {
            val tracking =
                it.where(Tracking::class.java).equalTo(Tracking.TRACKING_ID, id)
                    .findFirst()
            tracking?.deleteFromRealm()
            val preferences = Preferences.getInstance(context)
            preferences.putLong(Preferences.ON_DUTY_LAST_OPEN, 0L)
            preferences.putLong(Preferences.ON_DUTY, 0L)
        }
    }

    fun getFirstTracking(): Tracking? {
        return realm.where(Tracking::class.java).findFirst()
    }

    fun getTracking(): List<Tracking> {
        return realm.where(Tracking::class.java).findAll() ?: arrayListOf()
    }

    fun getCountTracking(): Int {
        return realm.where(Tracking::class.java).findAll().size
    }
}
