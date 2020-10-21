package org.rfcx.companion.view.detail

import org.rfcx.companion.R
import org.rfcx.companion.entity.DeploymentImage
import org.rfcx.companion.entity.SyncState

data class DeploymentImageView(
    val id: Int,
    val localPath: String,
    val remotePath: String?,
    var syncState: Int = 0 // syncToFireStoreState
) {
    val syncImage = when (syncState) {
        SyncState.Unsent.key -> R.drawable.ic_cloud_queue_16dp
        SyncState.Sending.key -> R.drawable.ic_cloud_upload_16dp
        else -> R.drawable.ic_cloud_done_16dp
    }
}

/**
 * @param syncState Return when wait upload, uploading to Firebase Storage and when uploaded to Firestore
 * */
fun DeploymentImage.toDeploymentImageView(): DeploymentImageView {
    return DeploymentImageView(
        id = this.id,
        localPath = this.localPath,
        remotePath = this.remotePath,
        syncState = if (this.syncToFireStoreState != SyncState.Sent.key) {
            this.syncState
        } else {
            this.syncToFireStoreState
        }
    )
}
