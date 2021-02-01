package org.rfcx.companion.localdb.guardian

import android.util.Log
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.entity.response.GuardianDeploymentResponse
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
            deployment.location = location // add deploy location
            it.insertOrUpdate(deployment)
        }
        return id
    }

    fun insertOrUpdate(deploymentResponse: GuardianDeploymentResponse) {
        realm.executeTransaction {
            val deployment =
                it.where(GuardianDeployment::class.java)
                    .equalTo(GuardianDeployment.FIELD_SERVER_ID, deploymentResponse.serverId)
                    .findFirst()

            if (deployment != null) {
                deployment.serverId = deploymentResponse.serverId
                deployment.deployedAt = deploymentResponse.deployedAt ?: deployment.deployedAt
                deployment.wifiName = deploymentResponse.wifi

                val newConfig = deploymentResponse.configuration
                if (newConfig != null) {
                    deployment.configuration = it.copyToRealm(newConfig)
                }

                val newLocation = deploymentResponse.location
                if (newLocation != null) {
                    deployment.location = it.copyToRealm(newLocation)
                }

                deployment.createdAt = deploymentResponse.createdAt ?: deployment.createdAt
            } else {
                val deploymentObj = deploymentResponse.toGuardianDeployment()
                val id = (it.where(GuardianDeployment::class.java).max(GuardianDeployment.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                deploymentObj.id = id
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
            realm.where(DeploymentImage::class.java).equalTo(DeploymentImage.FIELD_ID, deploymentId)
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
            if (guardianDeployment?.location != null) {
                guardianDeployment.location?.name = locationName
                guardianDeployment.location?.latitude = latitude
                guardianDeployment.location?.longitude = longitude
                guardianDeployment.location?.altitude = altitude
                guardianDeployment.updatedAt = Date()
                guardianDeployment.syncState = SyncState.Unsent.key
            }

            // do update location
            val location = if (guardianDeployment?.serverId != null) {
                bgRealm.where(Locate::class.java)
                    .equalTo(Locate.FIELD_LAST_EDGE_DEPLOYMENT_SERVER_ID, guardianDeployment.serverId)
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
            if (guardianDeployment?.location != null) {
                guardianDeployment.updatedAt = Date()
                guardianDeployment.syncState = SyncState.Unsent.key

                //update location group
                if (guardianDeployment.location?.locationGroup != null) {
                    guardianDeployment.location?.locationGroup?.let {
                        it.group = locationGroup.group
                        it.color = locationGroup.color
                        it.serverId = locationGroup.serverId
                    }
                } else {
                    val locationGroupObj = bgRealm.createObject(LocationGroup::class.java)
                    locationGroupObj.let {
                        it.color = locationGroup.color
                        it.group = locationGroup.group
                        it.serverId = locationGroup.serverId
                    }
                    guardianDeployment.location?.locationGroup = locationGroupObj
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
}
