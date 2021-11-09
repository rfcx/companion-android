package org.rfcx.companion.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.deleteFromRealm
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.response.DeploymentResponse
import org.rfcx.companion.entity.response.toDeploymentLocation
import org.rfcx.companion.entity.response.toDeployment
import java.util.*

class DeploymentDb(private val realm: Realm) {

    fun unsentCount(): Long {
        val audioMoths = realm.where(Deployment::class.java)
            .equalTo(Deployment.FIELD_DEVICE, Device.AUDIOMOTH.value)
            .and()
            .equalTo(Deployment.FIELD_STATE, DeploymentState.AudioMoth.ReadyToUpload.key)
            .and()
            .notEqualTo(Deployment.FIELD_SYNC_STATE, SyncState.Sent.key)
            .count()

        val guardians = realm.where(Deployment::class.java)
            .equalTo(Deployment.FIELD_DEVICE, Device.GUARDIAN.value)
            .and()
            .equalTo(Deployment.FIELD_STATE, DeploymentState.Guardian.ReadyToUpload.key)
            .and()
            .notEqualTo(Deployment.FIELD_SYNC_STATE, SyncState.Sent.key)
            .count()

        return audioMoths + guardians
    }

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<Deployment> {
        return realm.where(Deployment::class.java)
            .sort(Deployment.FIELD_ID, sort)
            .findAllAsync()
    }

    fun getDeployments(): List<Deployment> {
        return realm.where(Deployment::class.java)
            .sort(Deployment.FIELD_ID, Sort.DESCENDING)
            .findAll() ?: arrayListOf()
    }

    fun getAllResultsAsyncWithinProject(sort: Sort = Sort.DESCENDING, project: String): RealmResults<Deployment> {
        return realm.where(Deployment::class.java)
            .equalTo("stream.project.name", project)
            .sort(Deployment.FIELD_ID, sort)
            .findAllAsync()
    }

    fun deleteDeploymentByStreamId(id: String) {
        realm.executeTransaction {
            val deployments =
                it.where(Deployment::class.java).equalTo("stream.coreId", id)
                    .findAll()
            deployments.forEach { dp ->
                dp.isActive = false
                dp.syncState = SyncState.Unsent.key
            }
        }
    }

    fun insertOrUpdateDeployment(deployment: Deployment, location: DeploymentLocation): Int {
        var id = deployment.id
        realm.executeTransaction {
            if (deployment.id == 0) {
                id = (it.where(Deployment::class.java).max(Deployment.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                deployment.id = id
            }
            deployment.stream = it.copyToRealm(location)
            it.insertOrUpdate(deployment)
        }
        return id
    }

    fun insertOrUpdate(deploymentResponses: List<DeploymentResponse>) {
        realm.executeTransaction {
            deploymentResponses.forEach { deploymentResponse ->
                val deployment =
                    it.where(Deployment::class.java)
                        .equalTo(Deployment.FIELD_SERVER_ID, deploymentResponse.id)
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
                    val deploymentObj = deploymentResponse.toDeployment()
                    val id = (it.where(Deployment::class.java).max(Deployment.FIELD_ID)
                        ?.toInt() ?: 0) + 1
                    deploymentObj.id = id
                    it.insert(deploymentObj)
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

    fun updateDeployment(deployment: Deployment) {
        realm.executeTransaction {
            it.insertOrUpdate(deployment)
        }
    }

    fun getDeploymentById(id: Int): Deployment? {
        val deployment =
            realm.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id)
                .findFirst()
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
            val unsentGuardian = it.where(Deployment::class.java)
                .equalTo(Deployment.FIELD_DEVICE, Device.GUARDIAN.value)
                .and()
                .equalTo(Deployment.FIELD_STATE, DeploymentState.Guardian.ReadyToUpload.key)
                .and()
                .equalTo(Deployment.FIELD_SYNC_STATE, SyncState.Unsent.key).findAll()
                .createSnapshot()

            val unsentAudioMoth = it.where(Deployment::class.java)
                .equalTo(Deployment.FIELD_DEVICE, Device.AUDIOMOTH.value)
                .and()
                .equalTo(Deployment.FIELD_STATE, DeploymentState.AudioMoth.ReadyToUpload.key)
                .and()
                .equalTo(Deployment.FIELD_SYNC_STATE, SyncState.Unsent.key).findAll()
                .createSnapshot()

            unsentCopied = (unsentGuardian + unsentAudioMoth)
            unsentCopied.forEach { deployment ->
                deployment.syncState = SyncState.Sending.key
            }
        }
        return unsentCopied.toList()
    }

    fun unlockSending() {
        realm.executeTransaction {
            val snapshot = it.where(Deployment::class.java)
                .equalTo(Deployment.FIELD_SYNC_STATE, SyncState.Sending.key).findAll()
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
    fun editStream(
        id: Int,
        locationName: String,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        callback: DatabaseCallback
    ) {
        realm.executeTransactionAsync({ bgRealm ->
            // do update deployment location
            val deployment =
                bgRealm.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id)
                    .findFirst()
            if (deployment?.stream != null) {
                deployment.stream?.name = locationName
                deployment.stream?.latitude = latitude
                deployment.stream?.longitude = longitude
                deployment.stream?.altitude = altitude
                deployment.updatedAt = Date()
                deployment.syncState = SyncState.Unsent.key
            }
            val location = bgRealm.where(Locate::class.java)
                .equalTo(Locate.FIELD_LAST_DEPLOYMENT_ID, id).findFirst()

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

    fun editProject(id: Int, locationGroup: LocationGroup, callback: DatabaseCallback) {
        realm.executeTransactionAsync({ bgRealm ->
            // do update deployment location
            val guardianDeployment =
                bgRealm.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id)
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

    fun updateDeploymentByServerId(deployment: Deployment) {
        realm.executeTransaction {
            it.where(Deployment::class.java)
                .equalTo(Deployment.FIELD_SERVER_ID, deployment.serverId)
                .findFirst()?.apply {
                    stream?.coreId = deployment.stream?.coreId
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

    fun getDeploymentsBySiteId(streamId: String): ArrayList<Deployment> {
        val deployments = realm.where(Deployment::class.java)
            .equalTo(Deployment.FIELD_STATE, DeploymentState.Guardian.ReadyToUpload.key)
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

    fun deleteDeployment(id: Int) {
        realm.executeTransaction {
            val deployment =
                it.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id)
                    .findFirst()
            deployment?.deleteFromRealm()
        }
    }

    fun deleteDeploymentLocation(id: Int, callback: DatabaseCallback) {
        realm.executeTransactionAsync({ bgRealm ->
            // do update and set delete deployment
            val deployment =
                bgRealm.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id)
                    .findFirst()
            if (deployment?.stream != null) {
                deployment.deletedAt = Date()
                deployment.updatedAt = Date()
                deployment.syncState = SyncState.Unsent.key
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
