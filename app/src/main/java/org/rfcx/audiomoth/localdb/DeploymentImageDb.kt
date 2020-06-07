package org.rfcx.audiomoth.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.DeploymentImage
import org.rfcx.audiomoth.entity.SyncState

class DeploymentImageDb(private val realm: Realm) {

    fun unsentCount(): Long {
        return realm.where(DeploymentImage::class.java).notEqualTo(
            DeploymentImage.FIELD_SYNC_STATE,
            SyncState.Uploaded.key
        ).count()
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
}