package org.rfcx.audiomoth.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.deleteFromRealm
import org.rfcx.audiomoth.entity.*
import org.rfcx.audiomoth.entity.response.EdgeDeploymentResponse
import org.rfcx.audiomoth.entity.response.toEdgeDeployment
import java.util.*

/**
 * For Manage the saving and sending of deployment from the local database
 */
class EdgeDeploymentDb(private val realm: Realm) {

    fun unsentCount(): Long {
        return realm.where(EdgeDeployment::class.java)
            .equalTo(EdgeDeployment.FIELD_STATE, DeploymentState.Edge.ReadyToUpload.key)
            .and()
            .notEqualTo(EdgeDeployment.FIELD_SYNC_STATE, SyncState.Sent.key)
            .count()
    }

    fun getDeploymentsSent(): ArrayList<String> {
        val deployments = realm.where(EdgeDeployment::class.java)
            .equalTo(EdgeDeployment.FIELD_STATE, DeploymentState.Edge.ReadyToUpload.key)
            .and()
            .equalTo(EdgeDeployment.FIELD_SYNC_STATE, SyncState.Sent.key).findAllAsync()
        val arrayOfId = arrayListOf<String>()
        deployments.forEach {
            it.serverId?.let { it1 -> arrayOfId.add(it1) }
        }
        return arrayOfId
    }

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<EdgeDeployment> {
        return realm.where(EdgeDeployment::class.java)
            .sort(EdgeDeployment.FIELD_ID, sort)
            .findAllAsync()
    }

    fun insertOrUpdate(deployment: EdgeDeployment, location: DeploymentLocation): Int {
        var id = deployment.id
        realm.executeTransaction {
            if (deployment.id == 0) {
                id = (it.where(EdgeDeployment::class.java).max(EdgeDeployment.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                deployment.id = id
            }
            deployment.location = location // add deploy location
            it.insertOrUpdate(deployment)
        }
        return id
    }

    fun insertOrUpdate(deploymentResponse: EdgeDeploymentResponse) {
        realm.executeTransaction {
            val deployment =
                it.where(EdgeDeployment::class.java)
                    .equalTo(EdgeDeployment.FIELD_SERVER_ID, deploymentResponse.serverId)
                    .findFirst()

            if (deployment == null) {
                val deploymentObj = deploymentResponse.toEdgeDeployment()
                val id = (it.where(EdgeDeployment::class.java).max(EdgeDeployment.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                deploymentObj.id = id
                it.insert(deploymentObj)
            } else if (deployment.syncState == SyncState.Sent.key) {
                deployment.deploymentId = deploymentResponse.deploymentId
                deployment.serverId = deploymentResponse.serverId
                deployment.batteryDepletedAt =
                    deploymentResponse.batteryDepletedAt ?: deployment.batteryDepletedAt
                deployment.deployedAt = deploymentResponse.deployedAt ?: deployment.deployedAt
                deployment.batteryLevel = deploymentResponse.batteryLevel ?: deployment.batteryLevel

                val newConfig = deploymentResponse.configuration?.toEdgeConfiguration()
                if (newConfig != null) {
                    deployment.configuration = it.copyToRealm(newConfig)
                }

                val newLocation = deploymentResponse.location
                if (newLocation != null) {
                    deployment.location = it.copyToRealm(newLocation)
                }

                deployment.createdAt = deploymentResponse.createdAt ?: deployment.createdAt
            }
        }
    }

    fun markUnsent(id: Int) {
        mark(id = id, syncState = SyncState.Unsent.key)
    }

    fun markSent(serverId: String, id: Int) {
        mark(id, serverId, SyncState.Sent.key)
        saveDeploymentServerIdToImage(serverId, id)
    }

    private fun mark(id: Int, serverId: String? = null, syncState: Int) {
        realm.executeTransaction {
            val deployment =
                it.where(EdgeDeployment::class.java).equalTo(EdgeDeployment.FIELD_ID, id)
                    .findFirst()
            if (deployment != null) {
                deployment.serverId = serverId
                deployment.syncState = syncState
            }
        }
    }

    fun updateDeployment(deployment: EdgeDeployment) {
        realm.executeTransaction {
            it.insertOrUpdate(deployment)
        }
    }

    /**
     * Delete Locate
     * */
    fun deleteDeploymentLocation(id: Int, callback: DatabaseCallback) {
        realm.executeTransactionAsync({ bgRealm ->
            // do update and set delete deployment
            val edgeDeployment =
                bgRealm.where(EdgeDeployment::class.java).equalTo(EdgeDeployment.FIELD_ID, id)
                    .findFirst()
            if (edgeDeployment?.location != null) {
                edgeDeployment.deletedAt = Date()
                edgeDeployment.updatedAt = Date()
                edgeDeployment.syncState = SyncState.Unsent.key
            }

            // do set delete location
            val location = if (edgeDeployment?.serverId != null) {
                bgRealm.where(Locate::class.java)
                    .equalTo(Locate.FIELD_LAST_EDGE_DEPLOYMENT_SERVER_ID, edgeDeployment.serverId)
                    .findFirst()
            } else {
                bgRealm.where(Locate::class.java)
                    .equalTo(Locate.FIELD_LAST_EDGE_DEPLOYMENT_ID, id).findFirst()
            }

            if (location != null) {
                location.deletedAt = Date()
                location.syncState = SyncState.Unsent.key
            }
        }, {
            // success
            realm.close()
            callback.onSuccess()
        }, {
            // failure
            realm.close()
            callback.onFailure(it.localizedMessage ?: "")
        })
    }

    /**
     * Update Deployment Location and Locate
     * */
    fun editLocation(
        id: Int,
        locationName: String,
        latitude: Double,
        longitude: Double,
        callback: DatabaseCallback
    ) {
        realm.executeTransactionAsync({ bgRealm ->
            // do update deployment location
            val edgeDeployment =
                bgRealm.where(EdgeDeployment::class.java).equalTo(EdgeDeployment.FIELD_ID, id)
                    .findFirst()
            if (edgeDeployment?.location != null) {
                edgeDeployment.location?.name = locationName
                edgeDeployment.location?.latitude = latitude
                edgeDeployment.location?.longitude = longitude
                edgeDeployment.updatedAt = Date()
                edgeDeployment.syncState = SyncState.Unsent.key
            }

            // do update location
            val location = if (edgeDeployment?.serverId != null) {
                bgRealm.where(Locate::class.java)
                    .equalTo(Locate.FIELD_LAST_EDGE_DEPLOYMENT_SERVER_ID, edgeDeployment.serverId)
                    .findFirst()
            } else {
                bgRealm.where(Locate::class.java)
                    .equalTo(Locate.FIELD_LAST_EDGE_DEPLOYMENT_ID, id).findFirst()
            }

            if (location != null) {
                location.latitude = latitude
                location.longitude = longitude
                location.name = locationName
                location.syncState = SyncState.Unsent.key
            }
        }, {
            // success
            realm.close()
            callback.onSuccess()
        }, {
            // failure
            realm.close()
            callback.onFailure(it.localizedMessage ?: "")
        })
    }

    fun deleteDeployment(id: Int) {
        realm.executeTransaction {
            val deployment =
                it.where(EdgeDeployment::class.java).equalTo(EdgeDeployment.FIELD_ID, id)
                    .findFirst()
            deployment?.deleteFromRealm()
        }
    }

    fun getDeploymentById(id: Int): EdgeDeployment? {
        val deployment =
            realm.where(EdgeDeployment::class.java).equalTo(EdgeDeployment.FIELD_ID, id).findFirst()
        if (deployment != null) {
            return realm.copyFromRealm(deployment)
        }
        return null
    }

    fun getDeploymentByDeploymentId(deploymentId: String): EdgeDeployment? {
        val deployment =
            realm.where(EdgeDeployment::class.java)
                .equalTo(EdgeDeployment.FIELD_DEPLOYMENT_ID, deploymentId).findFirst()
        if (deployment != null) {
            return realm.copyFromRealm(deployment)
        }
        return null
    }

    fun lockUnsent(): List<EdgeDeployment> {
        var unsentCopied: List<EdgeDeployment> = listOf()
        realm.executeTransaction {
            val unsent = it.where(EdgeDeployment::class.java)
                .equalTo(EdgeDeployment.FIELD_STATE, DeploymentState.Edge.ReadyToUpload.key)
                .and()
                .equalTo(EdgeDeployment.FIELD_SYNC_STATE, SyncState.Unsent.key).findAll()
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
            val snapshot = it.where(EdgeDeployment::class.java)
                .equalTo(EdgeDeployment.FIELD_SYNC_STATE, SyncState.Sending.key).findAll()
                .createSnapshot()
            snapshot.forEach {
                it.syncState = SyncState.Unsent.key
            }
        }
    }

    private fun saveDeploymentServerIdToImage(serverId: String, deploymentId: Int) {
        val images =
            realm.where(DeploymentImage::class.java)
                .equalTo(DeploymentImage.FIELD_DEPLOYMENT_ID, deploymentId)
                .findAll()
        realm.executeTransaction { transition ->
            images?.forEach {
                val image = it.apply {
                    this.deploymentServerId = serverId
                }
                transition.insertOrUpdate(image)
            }
        }
    }
}
