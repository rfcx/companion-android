package org.rfcx.companion.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.deleteFromRealm
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.response.DeploymentResponse
import org.rfcx.companion.entity.response.toDeployment
import java.util.*

class DeploymentDb(private val realm: Realm) {

    fun unsentCount(): Long {
        return realm.where(Deployment::class.java)
            .equalTo(Deployment.FIELD_STATE, DeploymentState.Guardian.ReadyToUpload.key)
            .and()
            .notEqualTo(Deployment.FIELD_SYNC_STATE, SyncState.Sent.key)
            .count()
    }

    fun getUnsent(): List<Deployment> {
        return realm.where(Deployment::class.java)
            .equalTo(Deployment.FIELD_STATE, DeploymentState.Guardian.ReadyToUpload.key)
            .and()
            .notEqualTo(Deployment.FIELD_SYNC_STATE, SyncState.Sent.key)
            .sort(Deployment.FIELD_ID, Sort.DESCENDING)
            .findAll()
    }

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<Deployment> {
        return realm.where(Deployment::class.java)
            .sort(Deployment.FIELD_ID, sort)
            .findAllAsync()
    }

    fun getAllResultsAsyncWithinProject(sort: Sort = Sort.DESCENDING, id: Int): RealmResults<Deployment> {
        return realm.where(Deployment::class.java)
            .equalTo("stream.project.id", id)
            .sort(Deployment.FIELD_ID, sort)
            .findAllAsync()
    }

    fun deleteDeploymentByStreamId(id: String) {
        realm.executeTransaction {
            val deployments =
                it.where(Deployment::class.java).equalTo("stream.serverId", id)
                    .findAll()
            deployments.forEach { dp ->
                dp.isActive = false
                dp.syncState = SyncState.Unsent.key
            }
        }
    }

    fun insertOrUpdateDeployment(deployment: Deployment, streamId: Int): Int {
        var id = deployment.id
        realm.executeTransaction {
            if (deployment.id == 0) {
                id = (
                    it.where(Deployment::class.java).max(Deployment.FIELD_ID)
                        ?.toInt() ?: 0
                    ) + 1
                deployment.id = id
            }

            val stream = it.where(Stream::class.java).equalTo(Stream.FIELD_ID, streamId).findFirst()
            deployment.stream = stream
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

                    val streamObj = deploymentResponse.stream
                    if (streamObj != null) {
                        val stream = it.where(Stream::class.java).equalTo(Stream.FIELD_SERVER_ID, streamObj.id).findFirst()
                        deployment.stream = stream
                    }

                    deployment.createdAt = deploymentResponse.createdAt ?: deployment.createdAt
                } else {
                    val deploymentObj = deploymentResponse.toDeployment()
                    val streamObj = deploymentResponse.stream
                    if (streamObj != null) {
                        val stream = it.where(Stream::class.java).equalTo(Stream.FIELD_SERVER_ID, streamObj.id).findFirst()
                        deploymentObj.stream = stream
                    }

                    val id = (
                        it.where(Deployment::class.java).max(Deployment.FIELD_ID)?.toInt() ?: 0
                        ) + 1
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

            val unsentSongMeter = it.where(Deployment::class.java)
                .equalTo(Deployment.FIELD_DEVICE, Device.SONGMETER.value)
                .and()
                .equalTo(Deployment.FIELD_STATE, DeploymentState.AudioMoth.ReadyToUpload.key)
                .and()
                .equalTo(Deployment.FIELD_SYNC_STATE, SyncState.Unsent.key).findAll()
                .createSnapshot()

            unsentCopied = (unsentGuardian + unsentAudioMoth + unsentSongMeter)
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

    fun markNeedUpdate(id: Int) {
        realm.executeTransaction {
            val deployment =
                it.where(Deployment::class.java).equalTo(Deployment.FIELD_ID, id)
                    .findFirst()
            deployment?.updatedAt = Date()
            deployment?.syncState = SyncState.Unsent.key
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

    fun getDeploymentsBySiteId(streamId: Int, device: String): ArrayList<Deployment> {
        val deployments = realm.where(Deployment::class.java)
            .equalTo(Deployment.FIELD_STATE, if (device == Device.GUARDIAN.value) DeploymentState.Guardian.ReadyToUpload.key else DeploymentState.AudioMoth.ReadyToUpload.key)
            .and()
            .equalTo(Deployment.FIELD_SYNC_STATE, SyncState.Sent.key)
            .and()
            .equalTo("stream.id", streamId)
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
            callback.onSuccess()
        }, {
            // failure
            callback.onFailure(it.localizedMessage ?: "")
        })
    }
}
