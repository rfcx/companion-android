package org.rfcx.companion.localdb

import android.util.Log
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import org.rfcx.companion.entity.DeploymentImage
import org.rfcx.companion.entity.DeploymentImage.Companion.FIELD_DEPLOYMENT_ID
import org.rfcx.companion.entity.DeploymentImage.Companion.FIELD_DEPLOYMENT_SERVER_ID
import org.rfcx.companion.entity.DeploymentImage.Companion.FIELD_DEVICE
import org.rfcx.companion.entity.DeploymentImage.Companion.FIELD_ID
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.response.DeploymentAssetResponse
import org.rfcx.companion.view.deployment.Image

class DeploymentImageDb(private val realm: Realm) {

    fun unsentCount(): Long {
        return realm.where(DeploymentImage::class.java)
            .notEqualTo(DeploymentImage.FIELD_SYNC_STATE, SyncState.Sent.key)
            .count()
    }

    fun unlockSending() {
        realm.executeTransaction {
            val snapshot = it.where(DeploymentImage::class.java)
                .equalTo(DeploymentImage.FIELD_SYNC_STATE, SyncState.Sending.key).findAll()
                .createSnapshot()
            snapshot.forEach { profile ->
                profile.syncState = SyncState.Unsent.key
            }
        }
    }

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
        device: String = Device.AUDIOMOTH.value,
        sort: Sort = Sort.DESCENDING
    ): RealmResults<DeploymentImage> {
        return realm.where(DeploymentImage::class.java)
            .sort(FIELD_ID, sort)
            .equalTo(FIELD_DEPLOYMENT_ID, deploymentId)
            .equalTo(FIELD_DEVICE, device)
            .findAllAsync()
    }

    fun insertImage(
        deployment: Deployment? = null,
        attachImages: List<Image>
    ) {
        deployment?.let { dp ->
            val imageCreateAt = dp.deployedAt
            realm.executeTransaction {
                // save attached image to be Deployment Image
                attachImages.filter { it.path != null }.forEach { attachImage ->
                    val imageId =
                        (it.where(DeploymentImage::class.java).max(FIELD_ID)?.toInt() ?: 0) + 1
                    val deploymentImage = DeploymentImage(
                        id = imageId,
                        deploymentId = dp.id,
                        localPath = attachImage.path!!,
                        createdAt = imageCreateAt,
                        device = dp.device ?: "",
                        imageLabel = attachImage.name,
                        deploymentServerId = dp.serverId
                    )
                    Log.d("Comp", "insert $deploymentImage")
                    it.insertOrUpdate(deploymentImage)
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

    fun insertOrUpdate(
        deploymentAssetResponse: DeploymentAssetResponse,
        deploymentId: Int?,
        device: String
    ) {
        realm.executeTransaction {
            val image =
                it.where(DeploymentImage::class.java)
                    .equalTo(
                        DeploymentImage.FIELD_REMOTE_PATH,
                        "assets/${deploymentAssetResponse.id}"
                    )
                    .findFirst()

            if (image == null && deploymentId != null) {
                val deploymentImage = deploymentAssetResponse.toDeploymentImage()
                val id = (it.where(DeploymentImage::class.java).max(FIELD_ID)?.toInt() ?: 0) + 1
                deploymentImage.id = id
                deploymentImage.deploymentId = deploymentId
                deploymentImage.syncState = SyncState.Sent.key
                deploymentImage.device = device
                it.insert(deploymentImage)
            }
        }
    }
}
