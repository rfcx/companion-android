package org.rfcx.audiomoth.localdb

import io.realm.Realm
import org.rfcx.audiomoth.entity.Locate
import org.rfcx.audiomoth.entity.LocationGroup
import org.rfcx.audiomoth.entity.LocationGroups
import org.rfcx.audiomoth.entity.SyncState
import org.rfcx.audiomoth.entity.response.LocationGroupsResponse
import org.rfcx.audiomoth.entity.response.LocationResponse
import org.rfcx.audiomoth.entity.response.toLocate
import org.rfcx.audiomoth.entity.response.toLocationGroups

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
        mark(id, serverId, SyncState.Sent.key)
    }

    fun getLocationGroups(): List<LocationGroups> {
        return realm.where(LocationGroups::class.java).findAll() ?: arrayListOf()
    }

    fun getLocationGroups(name: String): LocationGroups {
        return realm.where(LocationGroups::class.java)
            .equalTo(LocationGroups.LOCATION_GROUPS_NAME, name).findFirst() ?: LocationGroups()
    }

    fun insertOrUpdate(groupsResponse: LocationGroupsResponse) {
        realm.executeTransaction {
            val group =
                it.where(LocationGroups::class.java)
                    .equalTo(LocationGroups.LOCATION_GROUPS_SERVER_ID, groupsResponse.serverId)
                    .findFirst()

            if (group == null) {
                val locationGroup = groupsResponse.toLocationGroups()
                val id = (it.where(LocationGroups::class.java).max(LocationGroups.LOCATION_GROUPS_ID)
                    ?.toInt() ?: 0) + 1
                locationGroup.id = id
                it.insert(locationGroup)
            } else if (group.syncState == SyncState.Sent.key) {
                group.serverId = groupsResponse.serverId
                group.name = groupsResponse.name
                group.color = groupsResponse.color
            }
        }
    }

    private fun mark(id: Int, serverId: String? = null, syncState: Int) {
        realm.executeTransaction {
            val locationGroup =
                it.where(LocationGroups::class.java).equalTo(LocationGroups.LOCATION_GROUPS_ID, id)
                    .findFirst()
            if (locationGroup != null) {
                locationGroup.serverId = serverId
                locationGroup.syncState = syncState
            }
        }
    }
}
