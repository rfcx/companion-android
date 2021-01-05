package org.rfcx.companion.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import org.rfcx.companion.entity.DeploymentImage
import org.rfcx.companion.entity.DeploymentImage.Companion.FIELD_DEPLOYMENT_ID
import org.rfcx.companion.entity.DeploymentImage.Companion.FIELD_DEPLOYMENT_SERVER_ID
import org.rfcx.companion.entity.DeploymentImage.Companion.FIELD_ID
import org.rfcx.companion.entity.EdgeDeployment
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.entity.response.DeploymentImageResponse

class DeploymentImageDb(private val realm: Realm) {

    fun getImageByDeploymentId(id: Int): List<DeploymentImage> {
        return realm.where(DeploymentImage::class.java)
            .equalTo(FIELD_DEPLOYMENT_ID, id)
            .findAll()
    }

    fun deleteImages(id: Int) {
        realm.executeTransaction {
            realm.where(DeploymentImage::class.java).equalTo(FIELD_DEPLOYMENT_ID, id)?.findAll()
                ?.deleteAllFromRealm()
        }
    }

    /**
     * return DeploymentImage that not be sync to Firebase Storage
     */
    fun lockUnsent(): List<DeploymentImage> {
        var unsentCopied: List<DeploymentImage> = listOf()
        realm.executeTransaction {
            val unsent = it.where(DeploymentImage::class.java)
                .equalTo(DeploymentImage.FIELD_SYNC_STATE, SyncState.Unsent.key)
                .isNotNull(FIELD_DEPLOYMENT_SERVER_ID)
                .findAll()
                .createSnapshot()
            unsentCopied = unsent.toList()
            unsent.forEach { d ->
                d.syncState = SyncState.Sending.key
            }
        }
        return unsentCopied
    }

    /**
     * Mark DeploymentImage.syncState to Unsent
     */
    fun markUnsent(id: Int) {
        realm.executeTransaction {
            val report = it.where(DeploymentImage::class.java).equalTo(FIELD_ID, id).findFirst()
            if (report != null) {
                report.syncState = SyncState.Unsent.key
            }
        }
    }

    /**
     * Mark DeploymentImage.syncState to Sent
     */
    fun markSent(id: Int, remotePath: String?) {
        realm.executeTransaction {
            val report = it.where(DeploymentImage::class.java).equalTo(FIELD_ID, id).findFirst()
            if (report != null) {
                report.syncState = SyncState.Sent.key
                report.remotePath = remotePath
            }
        }
    }

    /**
     * Return of RealmResults DeploymentImage By Deployment ID for Observer
     * */
    fun getAllResultsAsync(
        deploymentId: Int,
        sort: Sort = Sort.DESCENDING
    ): RealmResults<DeploymentImage> {
        return realm.where(DeploymentImage::class.java)
            .sort(FIELD_ID, sort)
            .equalTo(FIELD_DEPLOYMENT_ID, deploymentId)
            .findAllAsync()
    }

    fun insertImage(deployment: EdgeDeployment? = null, guardianDeployment: GuardianDeployment? = null, attachImages: List<String>) {
        if (deployment != null) {
            val imageCreateAt = deployment.deployedAt
            realm.executeTransaction {
                // save attached image to be Deployment Image
                attachImages.forEach { attachImage ->
                    val imageId =
                        (it.where(DeploymentImage::class.java).max(FIELD_ID)?.toInt() ?: 0) + 1
                    val deploymentImage = DeploymentImage(
                        id = imageId,
                        deploymentId = deployment.id,
                        localPath = attachImage,
                        createdAt = imageCreateAt
                    )
                    it.insertOrUpdate(deploymentImage)
                }
            }
        } else {
            if (guardianDeployment != null) {
                val imageCreateAt = guardianDeployment.deployedAt
                realm.executeTransaction {
                    // save attached image to be Deployment Image
                    attachImages.forEach { attachImage ->
                        val imageId =
                            (it.where(DeploymentImage::class.java).max(FIELD_ID)?.toInt() ?: 0) + 1
                        val deploymentImage = DeploymentImage(
                            id = imageId,
                            deploymentId = guardianDeployment.id,
                            localPath = attachImage,
                            createdAt = imageCreateAt
                        )
                        it.insertOrUpdate(deploymentImage)
                    }
                }
            }
        }
    }

    /**
     * Return of DeploymentImage that need to upload into firebase firestore
     */
    fun lockUnsentForFireStore(): List<DeploymentImage> {
        var unsentCopied: List<DeploymentImage> = listOf()
        realm.executeTransaction {
            val unsent = it.where(DeploymentImage::class.java)
                .equalTo(DeploymentImage.FIELD_SYNC_STATE, SyncState.Sent.key)
                .isNotNull(FIELD_DEPLOYMENT_SERVER_ID)
                .isNotNull("remotePath")
                .isNotEmpty("remotePath")
                .findAll()
            unsentCopied = unsent.createSnapshot()
            unsent.forEach { d ->
                d.syncToFireStoreState = SyncState.Sending.key
            }
        }
        return unsentCopied
    }

    /**
     * Mark DeploymentImage.syncToFireStoreState to Unsent
     */
    fun markUnsentFireStore(id: Int) {
        realm.executeTransaction {
            val report = it.where(DeploymentImage::class.java).equalTo(FIELD_ID, id).findFirst()
            if (report != null) {
                report.syncToFireStoreState = SyncState.Unsent.key
            }
        }
    }

    /**
     * Mark DeploymentImage.syncToFireStoreState to Sent
     */
    fun markSentFireStore(id: Int) {
        realm.executeTransaction {
            val report = it.where(DeploymentImage::class.java).equalTo(FIELD_ID, id).findFirst()
            if (report != null) {
                report.syncToFireStoreState = SyncState.Sent.key
            }
        }
    }

    fun insertOrUpdate(deploymentImageResponse: DeploymentImageResponse, deploymentId: Int?) {
        realm.executeTransaction {
            val image =
                it.where(DeploymentImage::class.java)
                    .equalTo(DeploymentImage.FIELD_REMOTE_PATH, deploymentImageResponse.remotePath)
                    .findFirst()

            if (image == null && deploymentId != null) {
                val deploymentImage = deploymentImageResponse.toDeploymentImage()
                val id = (it.where(DeploymentImage::class.java).max(FIELD_ID)?.toInt() ?: 0) + 1
                deploymentImage.id = id
                deploymentImage.deploymentId = deploymentId
                deploymentImage.syncState = SyncState.Sent.key
                deploymentImage.syncToFireStoreState = SyncState.Sent.key
                it.insert(deploymentImage)
            }
        }
    }
}
