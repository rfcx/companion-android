package org.rfcx.audiomoth.localdb

import android.util.Log
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import org.rfcx.audiomoth.entity.*
import org.rfcx.audiomoth.entity.response.DeploymentResponse
import org.rfcx.audiomoth.entity.response.toDeployment

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

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<Deployment> {
        return realm.where(Deployment::class.java)
            .sort(Deployment.FIELD_ID, sort)
            .findAllAsync()
    }

    fun insertOrUpdate(deployment: Deployment, location: DeploymentLocation): Int {
        var id = deployment.id
        realm.executeTransaction {
            if (deployment.id == 0) {
                id = (it.where(Deployment::class.java).max(Deployment.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                deployment.id = id
            }
            deployment.location = location // add deploy location
            it.insertOrUpdate(deployment)
        }
        return id
    }

    fun insertOrUpdate(deploymentResponse: DeploymentResponse) {
        realm.executeTransaction {
            val deployment =
                it.where(Deployment::class.java)
                    .equalTo(Deployment.FIELD_SERVER_ID, deploymentResponse.serverId)
                    .findFirst()

            if (deployment != null) {
                deployment.deploymentId = deploymentResponse.deploymentId
                deployment.serverId = deploymentResponse.serverId
                deployment.batteryDepletedAt =
                    deploymentResponse.batteryDepletedAt ?: deployment.batteryDepletedAt
                deployment.deployedAt = deploymentResponse.deployedAt ?: deployment.deployedAt
                deployment.batteryLevel = deploymentResponse.batteryLevel ?: deployment.batteryLevel

                val newConfig = deploymentResponse.configuration?.toConfiguration()
                if (newConfig != null) {
                    deployment.configuration = it.copyToRealm(newConfig)
                }

                val newLocation = deploymentResponse.location
                if (newLocation != null) {
                    deployment.location = it.copyToRealm(newLocation)
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
                it.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id).findFirst()
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
            realm.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id).findFirst()
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
        images?.forEach {
            Log.i("saveDeploymentIdToImage", it.localPath)
        }
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