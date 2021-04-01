package org.rfcx.companion.localdb.guardian

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.entity.response.DeploymentResponse
import org.rfcx.companion.entity.response.toDeploymentLocation
import org.rfcx.companion.entity.response.toGuardianDeployment
import org.rfcx.companion.localdb.DatabaseCallback
import java.util.*

class GuardianDeploymentDb(private val realm: Realm) {

    fun unsentCount(): Long {
        return realm.where(GuardianDeployment::class.java)
            .equalTo(GuardianDeployment.FIELD_STATE, DeploymentState.Guardian.ReadyToUpload.key)
            .and()
            .notEqualTo(GuardianDeployment.FIELD_SYNC_STATE, SyncState.Sent.key)
            .count()
    }

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<GuardianDeployment> {
        return realm.where(GuardianDeployment::class.java)
            .sort(GuardianDeployment.FIELD_ID, sort)
            .findAllAsync()
    }

    fun getAll(sort: Sort = Sort.DESCENDING): RealmResults<GuardianDeployment> {
        return realm.where(GuardianDeployment::class.java)
            .isNotNull(GuardianDeployment.FIELD_SERVER_ID)
            .sort(GuardianDeployment.FIELD_ID, sort)
            .findAll()
    }

    fun insertOrUpdateDeployment(
        deployment: GuardianDeployment,
        location: DeploymentLocation
    ): Int {
        var id = deployment.id
        realm.executeTransaction {
            if (deployment.id == 0) {
                id = (it.where(GuardianDeployment::class.java).max(GuardianDeployment.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                deployment.id = id
            }
            deployment.stream = it.copyToRealm(location)
        }
        return id
    }

    fun insertOrUpdate(deploymentResponse: DeploymentResponse) {
        realm.executeTransaction {
            val deployment =
                it.where(GuardianDeployment::class.java)
                    .equalTo(GuardianDeployment.FIELD_SERVER_ID, deploymentResponse.id)
                    .findFirst()

            if (deployment != null) {
                deployment.serverId = deploymentResponse.id
                deployment.deployedAt = deploymentResponse.deployedAt ?: deployment.deployedAt
                deployment.wifiName = deploymentResponse.wifi ?: ""

                val newConfig = deploymentResponse.configuration
                if (newConfig != null) {
                    deployment.configuration = it.copyToRealm(newConfig)
                }

                val newLocation = deploymentResponse.stream
                if (newLocation != null) {
                    deployment.stream = it.copyToRealm(newLocation.toDeploymentLocation())
                }

                deployment.createdAt = deploymentResponse.createdAt ?: deployment.createdAt
            } else {
                val deploymentObj = deploymentResponse.toGuardianDeployment()
                val id = (it.where(GuardianDeployment::class.java).max(GuardianDeployment.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                deploymentObj.id = id
                deploymentObj.isActive = true
                it.insert(deploymentObj)
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
                it.where(GuardianDeployment::class.java).equalTo(GuardianDeployment.FIELD_ID, id)
                    .findFirst()
            if (deployment != null) {
                deployment.serverId = serverId
                deployment.syncState = syncState
            }
        }
    }

    fun updateDeployment(deployment: GuardianDeployment) {
        realm.executeTransaction {
            it.insertOrUpdate(deployment)
        }
    }

    fun getDeploymentById(id: Int): GuardianDeployment? {
        val deployment =
            realm.where(GuardianDeployment::class.java).equalTo(GuardianDeployment.FIELD_ID, id)
                .findFirst()
        if (deployment != null) {
            return realm.copyFromRealm(deployment)
        }
        return null
    }

    fun getDeploymentByServerId(serverId: String): GuardianDeployment? {
        val deployment =
            realm.where(GuardianDeployment::class.java)
                .equalTo(GuardianDeployment.FIELD_SERVER_ID, serverId).findFirst()
        if (deployment != null) {
            return realm.copyFromRealm(deployment)
        }
        return null
    }

    fun lockUnsent(): List<GuardianDeployment> {
        var unsentCopied: List<GuardianDeployment> = listOf()
        realm.executeTransaction {
            val unsent = it.where(GuardianDeployment::class.java)
                .equalTo(GuardianDeployment.FIELD_STATE, DeploymentState.Guardian.ReadyToUpload.key)
                .and()
                .equalTo(GuardianDeployment.FIELD_SYNC_STATE, SyncState.Unsent.key).findAll()
                .createSnapshot()
            unsentCopied = unsent.toList()
            unsent.forEach { deployment ->
                deployment.syncState = SyncState.Sending.key
            }
        }
        return unsentCopied
    }

    fun unlockSending() {
        realm.executeTransaction {
            val snapshot = it.where(GuardianDeployment::class.java)
                .equalTo(GuardianDeployment.FIELD_SYNC_STATE, SyncState.Sending.key).findAll()
                .createSnapshot()
            snapshot.forEach { deployment ->
                deployment.syncState = SyncState.Unsent.key
            }
        }
    }

    private fun saveDeploymentServerIdToImage(serverId: String, deploymentId: Int) {
        val images =
            realm.where(DeploymentImage::class.java).equalTo(DeploymentImage.FIELD_DEPLOYMENT_ID, deploymentId)
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

    /**
     * Update Deployment Location and Locate
     * */
    fun editGuardianLocation(
        id: Int,
        locationName: String,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        callback: DatabaseCallback
    ) {
        realm.executeTransactionAsync({ bgRealm ->
            // do update deployment location
            val guardianDeployment =
                bgRealm.where(GuardianDeployment::class.java).equalTo(GuardianDeployment.FIELD_ID, id)
                    .findFirst()
            if (guardianDeployment?.stream != null) {
                guardianDeployment.stream?.name = locationName
                guardianDeployment.stream?.latitude = latitude
                guardianDeployment.stream?.longitude = longitude
                guardianDeployment.stream?.altitude = altitude
                guardianDeployment.updatedAt = Date()
                guardianDeployment.syncState = SyncState.Unsent.key
            }

            // do update location
            val location = if (guardianDeployment?.serverId != null) {
                bgRealm.where(Locate::class.java)
                    .equalTo(Locate.FIELD_LAST_GUARDIAN_DEPLOYMENT_SERVER_ID, guardianDeployment.serverId)
                    .findFirst()
            } else {
                bgRealm.where(Locate::class.java)
                    .equalTo(Locate.FIELD_LAST_GUARDIAN_DEPLOYMENT_ID, id).findFirst()
            }

            if (location != null) {
                location.latitude = latitude
                location.longitude = longitude
                location.altitude = altitude
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

    fun editLocationGroup(id: Int, locationGroup: LocationGroup, callback: DatabaseCallback) {
        realm.executeTransactionAsync({ bgRealm ->
            // do update deployment location
            val guardianDeployment =
                bgRealm.where(GuardianDeployment::class.java).equalTo(GuardianDeployment.FIELD_ID, id)
                    .findFirst()
            if (guardianDeployment?.stream != null) {
                guardianDeployment.updatedAt = Date()
                guardianDeployment.syncState = SyncState.Unsent.key

                //update location group
                if (guardianDeployment.stream?.project != null) {
                    guardianDeployment.stream?.project?.let {
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
                    guardianDeployment.stream?.project = locationGroupObj
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

    fun updateDeploymentByServerId(deployment: GuardianDeployment) {
        realm.executeTransaction {
            it.where(GuardianDeployment::class.java)
                .equalTo(GuardianDeployment.FIELD_SERVER_ID, deployment.serverId)
                .findFirst()?.apply {
                    stream?.coreId = deployment.stream?.coreId
                }
        }
    }

    fun updateIsActive(id: Int) {
        realm.executeTransaction {
            val deployment =
                it.where(GuardianDeployment::class.java).equalTo(GuardianDeployment.FIELD_ID, id)
                    .findFirst()
            if (deployment != null) {
                deployment.isActive = false
            }
        }
    }

    fun getDeploymentsBySiteId(streamId: String): ArrayList<GuardianDeployment> {
        val deployments = realm.where(GuardianDeployment::class.java)
            .equalTo(GuardianDeployment.FIELD_STATE, DeploymentState.Guardian.ReadyToUpload.key)
            .and()
            .equalTo(GuardianDeployment.FIELD_SYNC_STATE, SyncState.Sent.key)
            .and()
            .equalTo("stream.coreId", streamId)
            .findAllAsync()
        val arrayOfId = arrayListOf<GuardianDeployment>()
        deployments.forEach {
            it?.let { it1 -> arrayOfId.add(it1) }
        }
        return arrayOfId
    }
}
