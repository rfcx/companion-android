package org.rfcx.audiomoth.localdb

import android.util.Log
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.Locate
import org.rfcx.audiomoth.entity.SyncState
import org.rfcx.audiomoth.entity.response.LocationResponse
import org.rfcx.audiomoth.entity.response.toLocate

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
        return realm.where(Locate::class.java).equalTo(Locate.FIELD_ID, id)
            .findFirst()
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

    fun insertOrUpdateLocate(deploymentId: Int, locate: Locate) {
        realm.executeTransaction {
            if (locate.id == 0) {
                val id = (realm.where(Locate::class.java).max(Locate.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                locate.id = id
            }
            // update last deployment
            locate.lastDeploymentId = deploymentId
            it.insertOrUpdate(locate)
        }
    }

    fun updateDeploymentServerId(
        deploymentId: Int,
        deploymentServerId: String,
        isGuardian: Boolean = false
    ) {
        Log.e("LocationSyncWorker", "$deploymentId  $deploymentServerId")
        realm.executeTransaction {
            it.where(Locate::class.java).equalTo("lastDeployment", deploymentId)
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

            if (location != null) {
                location.serverId = locationResponse.serverId
                location.name = locationResponse.name ?: location.name
                location.latitude = locationResponse.latitude ?: location.latitude
                location.longitude = locationResponse.longitude ?: location.longitude
                location.createdAt = locationResponse.createdAt ?: location.createdAt
                location.lastDeploymentServerId = locationResponse.lastDeploymentServerId
            } else {
                val locate = locationResponse.toLocate()
                val id = (it.where(Locate::class.java).max(Deployment.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                locate.id = id
                it.insert(locate)
            }
        }
    }
}