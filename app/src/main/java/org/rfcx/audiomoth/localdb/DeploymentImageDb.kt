package org.rfcx.audiomoth.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.DeploymentImage
import org.rfcx.audiomoth.entity.DeploymentImage.Companion.FIELD_DEPLOYMENT_SERVER_ID
import org.rfcx.audiomoth.entity.DeploymentImage.Companion.FIELD_ID
import org.rfcx.audiomoth.entity.SyncState

class DeploymentImageDb(private val realm: Realm) {

    fun unsentCount(): Long {
        return realm.where(DeploymentImage::class.java).notEqualTo(
            DeploymentImage.FIELD_SYNC_STATE,
            SyncState.Sent.key
        ).count()
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

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<DeploymentImage> {
        return realm.where(DeploymentImage::class.java)
            .sort(DeploymentImage.FIELD_ID, sort)
            .findAllAsync()
    }

    fun insertImage(deployment: Deployment, attachImages: List<String>) {
        val imageCreateAt = deployment.deployedAt
        realm.executeTransaction {
            // save attached image to be Deployment Image
            attachImages.forEach { attachImage ->
                val imageId = (it.where(DeploymentImage::class.java).max(
                    DeploymentImage.FIELD_ID
                )?.toInt() ?: 0) + 1
                val deploymentImage = DeploymentImage(
                    id = imageId,
                    deploymentId = deployment.id,
                    localPath = attachImage,
                    createdAt = imageCreateAt
                )
                it.insertOrUpdate(deploymentImage)
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
                d.syncState = SyncState.Sending.key
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
}