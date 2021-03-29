package org.rfcx.companion.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.deleteFromRealm
import org.rfcx.companion.entity.LocationGroups
import org.rfcx.companion.entity.LocationGroups.Companion.LOCATION_GROUPS_DELETE_AT
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.entity.response.ProjectResponse
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
        return realm.where(LocationGroups::class.java).isNull(LOCATION_GROUPS_DELETE_AT)
            .sort(LocationGroups.LOCATION_GROUPS_NAME, Sort.ASCENDING).findAll()
            ?: arrayListOf()
    }

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<LocationGroups> {
        return realm.where(LocationGroups::class.java)
            .sort(LocationGroups.LOCATION_GROUPS_ID, sort)
            .findAllAsync()
    }

    fun getLocationGroup(name: String): LocationGroups {
        return realm.where(LocationGroups::class.java)
            .equalTo(LocationGroups.LOCATION_GROUPS_NAME, name).findFirst() ?: LocationGroups()
    }

    fun insertOrUpdate(groupsResponse: ProjectResponse) {
        realm.executeTransaction {
            val group =
                it.where(LocationGroups::class.java)
                    .equalTo(LocationGroups.LOCATION_GROUPS_SERVER_ID, groupsResponse.id)
                    .findFirst()

            if (group == null) {
                val locationGroup = groupsResponse.toLocationGroups()
                val id =
                    (it.where(LocationGroups::class.java).max(LocationGroups.LOCATION_GROUPS_ID)
                        ?.toInt() ?: 0) + 1
                locationGroup.id = id
                it.insert(locationGroup)
            } else if (group.syncState == SyncState.Sent.key) {
                group.serverId = groupsResponse.id
                group.name = groupsResponse.name
                group.color = groupsResponse.color
            }
        }
    }

    fun deleteLocationGroup(id: Int, callback: DatabaseCallback) {
        realm.executeTransactionAsync({ bgRealm ->
            val locationGroup =
                bgRealm.where(LocationGroups::class.java)
                    .equalTo(LocationGroups.LOCATION_GROUPS_ID, id)
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

    fun isExisted(name: String?): Boolean {
        return if (name != null) {
            val locationGroup = realm.where(LocationGroups::class.java)
                .equalTo(LocationGroups.LOCATION_GROUPS_NAME, name).findFirst()
            locationGroup != null
        } else {
            false
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
