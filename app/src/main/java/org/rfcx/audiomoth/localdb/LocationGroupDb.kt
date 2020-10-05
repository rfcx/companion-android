package org.rfcx.audiomoth.localdb

import android.util.Log
import io.realm.Realm
import org.rfcx.audiomoth.entity.LocationGroups

class LocationGroupDb(private val realm: Realm) {
    fun insertOrUpdateLocationGroup(group: LocationGroups) {
        realm.executeTransaction {
            if (group.id == 0) {
                val id =
                    (realm.where(LocationGroups::class.java).max(LocationGroups.LOCATION_GROUPS_ID)
                        ?.toInt() ?: 0) + 1
                group.id = id
            }
            it.insertOrUpdate(group)
            Log.d("insertOrUpdate","insertOrUpdate")
        }
    }
}
