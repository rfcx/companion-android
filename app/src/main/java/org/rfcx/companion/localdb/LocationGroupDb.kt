package org.rfcx.companion.localdb

import io.realm.Realm
import io.realm.kotlin.deleteFromRealm
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.response.LocationGroupsResponse
import org.rfcx.companion.entity.response.toLocationGroups
import java.util.*

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

    fun getLocationGroup(name: String): LocationGroups {
        return realm.where(LocationGroups::class.java)
            .equalTo(LocationGroups.LOCATION_GROUPS_NAME, name).findFirst() ?: LocationGroups()
    }

    fun getFirstLocationGroup(): LocationGroups {
        return realm.where(LocationGroups::class.java)
            .notEqualTo(LocationGroups.LOCATION_GROUPS_NAME, "None")
            .findFirst() ?: LocationGroups()
    }

    fun insertOrUpdate(groupsResponse: LocationGroupsResponse) {
        realm.executeTransaction {
            if (groupsResponse.deletedAt == null) {
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
    }

    fun deleteLocationGroup(id: Int, callback: DatabaseCallback) {
        realm.executeTransactionAsync({ bgRealm ->
            val locationGroup =
                bgRealm.where(LocationGroups::class.java).equalTo(LocationGroups.LOCATION_GROUPS_ID, id)
                    .findFirst()
            if (locationGroup != null) {
                locationGroup.deletedAt = Date()
                locationGroup.syncState = SyncState.Unsent.key
            }
        }, {
            callback.onSuccess()
        }, {
            callback.onFailure(it.localizedMessage ?: "")
        })
    }

    fun deleteLocationGroupFromLocal(id: Int) {
        realm.executeTransaction {
            val locationGroup =
                it.where(LocationGroups::class.java).equalTo(LocationGroups.LOCATION_GROUPS_ID, id)
                    .findFirst()
            locationGroup?.deleteFromRealm()
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
