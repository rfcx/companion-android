package org.rfcx.companion.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.deleteFromRealm
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.response.DeploymentResponse
import org.rfcx.companion.entity.response.toDeploymentLocation
import org.rfcx.companion.entity.response.toEdgeDeployment
import java.util.*

/**
 * For Manage the saving and sending of deployment from the local database
 */
class DeploymentDb(private val realm: Realm) {

    fun unsentCount(): Long {
        return realm.where(Deployment::class.java)
            .equalTo(Deployment.FIELD_STATE, DeploymentState.Edge.ReadyToUpload.key)
            .and()
            .notEqualTo(Deployment.FIELD_SYNC_STATE, SyncState.Sent.key)
            .count()
    }

    fun getDeploymentsBySiteId(streamId: String): ArrayList<Deployment> {
        val deployments = realm.where(Deployment::class.java)
            .equalTo(Deployment.FIELD_STATE, DeploymentState.Edge.ReadyToUpload.key)
            .and()
            .equalTo(Deployment.FIELD_SYNC_STATE, SyncState.Sent.key)
            .and()
            .equalTo("stream.coreId", streamId)
            .findAllAsync()
        val arrayOfId = arrayListOf<Deployment>()
        deployments.forEach {
            it?.let { it1 -> arrayOfId.add(it1) }
        }
        return arrayOfId
    }

    fun getDeploymentBySiteName(name: String): Deployment? {
        val deployment =
            realm.where(Deployment::class.java).equalTo("stream.name", name).findFirst()
        if (deployment != null) {
            return realm.copyFromRealm(deployment)
        }
        return null
    }

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<Deployment> {
        return realm.where(Deployment::class.java)
            .sort(Deployment.FIELD_ID, sort)
            .findAllAsync()
    }

    fun getAllResultsAsyncWithinProject(sort: Sort = Sort.DESCENDING, project: String): RealmResults<Deployment> {
        return realm.where(Deployment::class.java)
            .equalTo("stream.project.name", project)
            .sort(Deployment.FIELD_ID, sort)
            .findAllAsync()
    }

    fun getAll(sort: Sort = Sort.DESCENDING): RealmResults<Deployment> {
        return realm.where(Deployment::class.java)
            .isNotNull(Deployment.FIELD_SERVER_ID)
            .sort(Deployment.FIELD_ID, sort)
            .findAll()
    }

    fun insertOrUpdate(deployment: Deployment, location: DeploymentLocation): Int {
        var id = deployment.id
        realm.executeTransaction {
            if (deployment.id == 0) {
                id = (it.where(Deployment::class.java).max(Deployment.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                deployment.id = id
            }
            deployment.stream = location // add deploy location
            it.insertOrUpdate(deployment)
        }
        return id
    }

    fun insertOrUpdate(deploymentResponse: DeploymentResponse) {
        realm.executeTransaction {
            val deployment =
                it.where(Deployment::class.java)
                    .equalTo(Deployment.FIELD_SERVER_ID, deploymentResponse.id)
                    .findFirst()

            if (deployment == null) {
                val deploymentObj = deploymentResponse.toEdgeDeployment()
                val id = (it.where(Deployment::class.java).max(Deployment.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                deploymentObj.id = id
                it.insert(deploymentObj)
            } else if (deployment.syncState == SyncState.Sent.key) {
                deployment.deploymentKey = deploymentResponse.id
                deployment.serverId = deploymentResponse.id
                deployment.deployedAt = deploymentResponse.deployedAt ?: deployment.deployedAt

                val newLocation = deploymentResponse.stream
                if (newLocation != null) {
                    deployment.stream = it.copyToRealm(newLocation.toDeploymentLocation())
                }

                deployment.createdAt =
                    deploymentResponse.createdAt ?: deployment.createdAt
            }
        }
    }

    fun insertOrUpdate(deploymentResponses: List<DeploymentResponse>) {
        realm.executeTransaction {
            deploymentResponses.forEach { deploymentResponse ->
                val deployment =
                    it.where(Deployment::class.java)
                        .equalTo(Deployment.FIELD_SERVER_ID, deploymentResponse.id)
                        .findFirst()

                if (deployment == null) {
                    val deploymentObj = deploymentResponse.toEdgeDeployment()
                    val id = (it.where(Deployment::class.java).max(Deployment.FIELD_ID)
                        ?.toInt() ?: 0) + 1
                    deploymentObj.id = id
                    it.insert(deploymentObj)
                } else if (deployment.syncState == SyncState.Sent.key) {
                    deployment.deploymentKey = deploymentResponse.id
                    deployment.serverId = deploymentResponse.id
                    deployment.deployedAt = deploymentResponse.deployedAt ?: deployment.deployedAt

                    val newLocation = deploymentResponse.stream
                    if (newLocation != null) {
                        deployment.stream = it.copyToRealm(newLocation.toDeploymentLocation())
                    }

                    deployment.createdAt =
                        deploymentResponse.createdAt ?: deployment.createdAt
                }
            }
        }
    }

    fun markUnsent(id: Int) {
        mark(id = id, syncState = SyncState.Unsent.key)
    }

    fun markSent(serverId: String, id: Int) {
        mark(id, serverId, SyncState.Sent.key)
        saveDeploymentServerIdToImage(serverId, id)
        saveDeploymentServerIdToTrack(serverId, id)
    }

    private fun mark(id: Int, serverId: String? = null, syncState: Int) {
        realm.executeTransaction {
            val deployment =
                it.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id)
                    .findFirst()
            if (deployment != null) {
                deployment.serverId = serverId
                deployment.syncState = syncState
            }
        }
    }

    fun updateIsActive(id: Int) {
        realm.executeTransaction {
            val deployment =
                it.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id)
                    .findFirst()
            if (deployment != null) {
                deployment.isActive = false
            }
        }
    }

    fun updateDeployment(deployment: Deployment) {
        realm.executeTransaction {
            it.insertOrUpdate(deployment)
        }
    }

    fun updateDeploymentByServerId(deployment: Deployment) {
        realm.executeTransaction {
            it.where(Deployment::class.java)
                .equalTo(Deployment.FIELD_SERVER_ID, deployment.serverId)
                .findFirst()?.apply {
                    stream?.coreId = deployment.stream?.coreId
                }
        }
    }

    /**
     * Delete Locate
     * */
    fun deleteDeploymentLocation(id: Int, callback: DatabaseCallback) {
        realm.executeTransactionAsync({ bgRealm ->
            // do update and set delete deployment
            val edgeDeployment =
                bgRealm.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id)
                    .findFirst()
            if (edgeDeployment?.stream != null) {
                edgeDeployment.deletedAt = Date()
                edgeDeployment.updatedAt = Date()
                edgeDeployment.syncState = SyncState.Unsent.key
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
        altitude: Double,
        callback: DatabaseCallback
    ) {
        realm.executeTransactionAsync({ bgRealm ->
            // do update deployment location
            val edgeDeployment =
                bgRealm.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id)
                    .findFirst()
            if (edgeDeployment?.stream != null) {
                edgeDeployment.stream?.name = locationName
                edgeDeployment.stream?.latitude = latitude
                edgeDeployment.stream?.longitude = longitude
                edgeDeployment.stream?.altitude = altitude
                edgeDeployment.updatedAt = Date()
                edgeDeployment.syncState = SyncState.Unsent.key
            }

            // do update location
            val location = if (edgeDeployment?.serverId != null) {
                bgRealm.where(Locate::class.java)
                    .equalTo(Locate.FIELD_LAST_EDGE_DEPLOYMENT_SERVER_ID, edgeDeployment.serverId)
                    .findFirst()
                    ?: bgRealm.where(Locate::class.java)
                        .equalTo(Locate.FIELD_SERVER_ID, edgeDeployment.stream?.coreId)
                        .findFirst()
            } else {
                bgRealm.where(Locate::class.java)
                    .equalTo(Locate.FIELD_LAST_EDGE_DEPLOYMENT_ID, id).findFirst()
            }

            if (location != null) {
                location.latitude = latitude
                location.longitude = longitude
                location.altitude = altitude
                location.name = locationName
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

    fun editLocationGroup(id: Int, locationGroup: LocationGroup, callback: DatabaseCallback) {
        realm.executeTransactionAsync({ bgRealm ->
            // do update deployment location
            val edgeDeployment =
                bgRealm.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id)
                    .findFirst()
            if (edgeDeployment?.stream != null) {
                edgeDeployment.updatedAt = Date()
                edgeDeployment.syncState = SyncState.Unsent.key

                //update location group
                if (edgeDeployment.stream?.project != null) {
                    edgeDeployment.stream?.project?.let {
                        it.name = locationGroup.name
                        it.color = locationGroup.color
                        it.coreId = locationGroup.coreId
                    }
                } else {
                    val locationGroupObj = bgRealm.createObject(LocationGroup::class.java)
                    locationGroupObj.let {
                        it.color = locationGroup.color
                        it.name = locationGroup.name
                        it.coreId = locationGroup.coreId
                    }
                    edgeDeployment.stream?.project = locationGroupObj
                }

                // do update location group
                val location = if (edgeDeployment.serverId != null) {
                    bgRealm.where(Locate::class.java)
                        .equalTo(
                            Locate.FIELD_LAST_EDGE_DEPLOYMENT_SERVER_ID,
                            edgeDeployment.serverId
                        )
                        .findFirst()
                } else {
                    bgRealm.where(Locate::class.java)
                        .equalTo(Locate.FIELD_LAST_EDGE_DEPLOYMENT_ID, id).findFirst()
                }

                if (location?.locationGroup != null) {
                    val groupLocation = location.locationGroup
                    if (groupLocation != null) {
                        groupLocation.name = locationGroup.name
                        groupLocation.color = locationGroup.color
                        groupLocation.coreId = locationGroup.coreId
                        location.syncState = SyncState.Unsent.key
                    }
                }
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
                it.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id)
                    .findFirst()
            deployment?.deleteFromRealm()
        }
    }

    fun deleteDeploymentByStreamId(id: String) {
        realm.executeTransaction {
            val deployments =
                it.where(Deployment::class.java).equalTo("stream.coreId", id)
                    .findAll()
            deployments.forEach { dp ->
                dp.deletedAt = Date()
                dp.isActive = false
                dp.syncState = SyncState.Unsent.key
            }
        }
    }

    fun getDeploymentById(id: Int): Deployment? {
        val deployment =
            realm.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id).findFirst()
        if (deployment != null) {
            return realm.copyFromRealm(deployment)
        }
        return null
    }

    fun getDeploymentByDeploymentId(deploymentId: String): Deployment? {
        val deployment =
            realm.where(Deployment::class.java)
                .equalTo(Deployment.FIELD_DEPLOYMENT_KEY, deploymentId).findFirst()
        if (deployment != null) {
            return realm.copyFromRealm(deployment)
        }
        return null
    }

    fun getDeploymentByServerId(serverId: String): Deployment? {
        val deployment =
            realm.where(Deployment::class.java)
                .equalTo(Deployment.FIELD_SERVER_ID, serverId).findFirst()
        if (deployment != null) {
            return realm.copyFromRealm(deployment)
        }
        return null
    }

    fun lockUnsent(): List<Deployment> {
        var unsentCopied: List<Deployment> = listOf()
        realm.executeTransaction {
            val unsent = it.where(Deployment::class.java)
                .equalTo(Deployment.FIELD_STATE, DeploymentState.Edge.ReadyToUpload.key)
                .and()
                .equalTo(Deployment.FIELD_SYNC_STATE, SyncState.Unsent.key).findAll()
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
            val snapshot = it.where(Deployment::class.java)
                .equalTo(Deployment.FIELD_SYNC_STATE, SyncState.Sending.key).findAll()
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

    private fun saveDeploymentServerIdToTrack(serverId: String, deploymentId: Int) {
        val file =
            realm.where(TrackingFile::class.java)
                .equalTo(TrackingFile.FIELD_DEPLOYMENT_ID, deploymentId)
                .findAll()
        realm.executeTransaction { transition ->
            file?.forEach {
                val image = it.apply {
                    this.deploymentServerId = serverId
                }
                transition.insertOrUpdate(image)
            }
        }
    }
}
