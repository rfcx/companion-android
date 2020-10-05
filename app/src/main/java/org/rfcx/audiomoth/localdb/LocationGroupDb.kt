package org.rfcx.audiomoth.localdb

import io.realm.Realm
import org.rfcx.audiomoth.entity.LocationGroups
import org.rfcx.audiomoth.entity.SyncState

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
        }
    }

    fun unsentCount(): Long {
        return realm.where(LocationGroups::class.java)
            .notEqualTo(LocationGroups.LOCATION_GROUPS_SYNC_STATE, SyncState.Sent.key)
            .count()
    }

    fun unlockSent(): List<LocationGroups> {
        var unsentCopied: List<LocationGroups> = listOf()
        realm.executeTransaction {
            val unsent = it.where(LocationGroups::class.java)
                .equalTo(LocationGroups.LOCATION_GROUPS_SYNC_STATE, SyncState.Unsent.key).findAll()
                .createSnapshot()
            unsentCopied = unsent.toList()
            unsent.forEach { d ->
                d.syncState = SyncState.Sending.key
            }
        }
        return unsentCopied
    }

    fun unlockSending() {
        realm.executeTransaction {
            val snapshot = it.where(LocationGroups::class.java)
                .equalTo(LocationGroups.LOCATION_GROUPS_SYNC_STATE, SyncState.Sending.key).findAll()
                .createSnapshot()
            snapshot.forEach {
                it.syncState = SyncState.Unsent.key
            }
        }
    }

    fun markUnsent(id: Int) {
        mark(id = id, syncState = SyncState.Unsent.key)
    }

    fun markSent(serverId: String, id: Int) {
        mark(id, SyncState.Sent.key)
    }

    private fun mark(id: Int, syncState: Int) {
        realm.executeTransaction {
            val locationGroup =
                it.where(LocationGroups::class.java).equalTo(LocationGroups.LOCATION_GROUPS_ID, id)
                    .findFirst()
            if (locationGroup != null) {
                locationGroup.syncState = syncState
            }
        }
    }
}
