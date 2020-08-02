package org.rfcx.audiomoth.localdb

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

    fun getLocateByServerId(serverId: String): Locate? {
        val locate =
            realm.where(Locate::class.java)
                .equalTo(Locate.FIELD_LAST_DEPLOYMENT_SERVER_ID, serverId).findFirst()
        if (locate != null) {
            return realm.copyFromRealm(locate)
        }
        return null
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

    fun getLocatesSend(): ArrayList<String> {
        val sentCopied = arrayListOf<String>()
        realm.executeTransaction {
            val unsent = it.where(Locate::class.java)
                .equalTo("syncState", SyncState.Sent.key)
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

            unsent.forEach { locate ->
                locate.serverId?.let { serverId -> sentCopied.add(serverId) }
            }
        }
        return sentCopied
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

    fun updateLocate(locate: Locate) {
        realm.executeTransaction {
            it.insertOrUpdate(locate)
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
                val id = (it.where(Locate::class.java).max(Deployment.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                locate.id = id
                it.insert(locate)
            } else if (location.syncState == SyncState.Sent.key) {
                location.serverId = locationResponse.serverId
                location.name = locationResponse.name ?: location.name
                location.latitude = locationResponse.latitude ?: location.latitude
                location.longitude = locationResponse.longitude ?: location.longitude
                location.createdAt = locationResponse.createdAt ?: location.createdAt
                location.lastDeploymentServerId = locationResponse.lastDeploymentServerId
                location.lastGuardianDeploymentServerId =
                    locationResponse.lastGuardianDeploymentServerId
            }
        }
    }
}
