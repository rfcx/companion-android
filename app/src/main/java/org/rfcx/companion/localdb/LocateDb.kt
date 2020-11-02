package org.rfcx.companion.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.deleteFromRealm
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.LocationGroup
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.entity.response.LocationResponse
import org.rfcx.companion.entity.response.toLocate

class LocateDb(private val realm: Realm) {

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<Locate> {
        return realm.where(Locate::class.java)
            .sort(Locate.FIELD_ID, sort)
            .findAllAsync()
    }

    fun getLocations(): List<Locate> {
        return realm.where(Locate::class.java).findAll() ?: arrayListOf()
    }

    fun getLocateById(id: Int): Locate? {
        return realm.where(Locate::class.java).equalTo(Locate.FIELD_ID, id).findFirst()
    }

    fun getLocateByEdgeDeploymentId(deploymentId: Int): Locate? {
        return realm.where(Locate::class.java)
            .equalTo(Locate.FIELD_LAST_EDGE_DEPLOYMENT_ID, deploymentId).findFirst()
    }

    fun getLocateByServerId(serverId: String): Locate? {
        val locate =
            realm.where(Locate::class.java)
                .equalTo(Locate.FIELD_LAST_EDGE_DEPLOYMENT_SERVER_ID, serverId).findFirst()
        if (locate != null) {
            return realm.copyFromRealm(locate)
        }
        return null
    }

    fun getDeleteLocateId(name: String, latitude: Double, longitude: Double): Int? {
        var locateId: Int? = null
        realm.executeTransaction {
            val locate = it.where(Locate::class.java)
                .equalTo("name", name)
                .and()
                .equalTo("latitude", latitude)
                .and()
                .equalTo("longitude", longitude)
                .findFirst()
            locateId = locate?.id
        }
        return locateId
    }

    fun deleteLocate(id: Int) {
        realm.executeTransaction {
            val locate =
                it.where(Locate::class.java).equalTo(Locate.FIELD_ID, id)
                    .findFirst()
            locate?.deleteFromRealm()
        }
    }

    fun unlockSent(): List<Locate> {
        var unsentCopied: List<Locate> = listOf()
        realm.executeTransaction {
            val unsent = it.where(Locate::class.java)
                .equalTo("syncState", SyncState.Unsent.key)
                .and()
                .isNotNull("lastDeploymentServerId")
                .and()
                .isNotEmpty("lastDeploymentServerId")
                .or()
                .isNotNull("lastGuardianDeploymentServerId")
                .and()
                .isNotEmpty("lastGuardianDeploymentServerId")
                .findAll()
                .createSnapshot()
            unsentCopied = unsent.toList()
            unsent.forEach { locate ->
                locate.syncState = SyncState.Sending.key
            }
        }
        return unsentCopied
    }

    fun markUnsent(id: Int) {
        mark(id = id, syncState = SyncState.Unsent.key)
    }

    fun markSent(serverId: String, id: Int) {
        mark(id, serverId, SyncState.Sent.key)
    }

    fun markUploading(id: Int) {
        mark(id = id, syncState = SyncState.Sending.key)
    }

    private fun mark(id: Int, serverId: String? = null, syncState: Int) {
        realm.executeTransaction {
            val locate =
                it.where(Locate::class.java).equalTo(Locate.FIELD_ID, id).findFirst()
            if (locate != null) {
                locate.serverId = serverId
                locate.syncState = syncState
            }
        }
    }

    fun insertOrUpdateLocate(deploymentId: Int, locate: Locate, isGuardian: Boolean = false) {
        realm.executeTransaction {
            if (locate.id == 0) {
                val id = (realm.where(Locate::class.java).max(Locate.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                locate.id = id
            }
            // new last deployment?
            if (!isGuardian) {
                locate.lastDeploymentId = deploymentId
                locate.lastGuardianDeploymentId = 0
                locate.lastGuardianDeploymentServerId = null
            } else {
                locate.lastGuardianDeploymentId = deploymentId
                locate.lastDeploymentId = 0
                locate.lastDeploymentServerId = null
            }
            it.insertOrUpdate(locate)
        }
    }

    fun updateDeploymentServerId(
        deploymentId: Int,
        deploymentServerId: String,
        isGuardian: Boolean = false
    ) {
        realm.executeTransaction {
            it.where(Locate::class.java).equalTo(
                if (!isGuardian) "lastDeploymentId" else "lastGuardianDeploymentId",
                deploymentId
            )
                .findFirst()?.apply {
                    if (!isGuardian) {
                        lastDeploymentServerId = deploymentServerId
                    } else {
                        lastGuardianDeploymentServerId = deploymentServerId
                    }
                    // set sync state to unsent after deploymentServerId have been change
                    syncState = SyncState.Unsent.key
                }
        }
    }

    fun insertOrUpdate(locationResponse: LocationResponse) {
        realm.executeTransaction {
            val location =
                it.where(Locate::class.java)
                    .equalTo(Locate.FIELD_SERVER_ID, locationResponse.serverId)
                    .findFirst()

            if (location == null) {
                val locate = locationResponse.toLocate()
                val id = (it.where(Locate::class.java).max(Locate.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                locate.id = id
                it.insert(locate)
            } else if (location.syncState == SyncState.Sent.key) {
                location.serverId = locationResponse.serverId
                location.name = locationResponse.name ?: location.name
                location.latitude = locationResponse.latitude ?: location.latitude
                location.longitude = locationResponse.longitude ?: location.longitude
                location.createdAt = locationResponse.createdAt ?: location.createdAt

                val locationGroupObj = it.createObject(LocationGroup::class.java)
                locationGroupObj?.let { obj ->
                    val locationGroup = locationResponse.locationGroup
                    if (locationGroup != null) {
                        obj.group = locationGroup.group
                        obj.color = locationGroup.color
                        obj.serverId = locationGroup.serverId
                    }
                }
                location.locationGroup = locationGroupObj

                location.lastDeploymentServerId = locationResponse.lastDeploymentServerId
                location.lastGuardianDeploymentServerId =
                    locationResponse.lastGuardianDeploymentServerId
            }
        }
    }
}