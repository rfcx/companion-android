package org.rfcx.companion.view.detail

import org.rfcx.companion.BuildConfig
import org.rfcx.companion.R
import org.rfcx.companion.entity.DeploymentImage
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.view.deployment.Image
import org.rfcx.companion.view.deployment.ImageType

data class DeploymentImageView(
    val id: Int,
    val localPath: String,
    val remotePath: String?,
    val label: String,
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
        remotePath = if (this.remotePath != null) BuildConfig.DEVICE_API_DOMAIN + this.remotePath else null,
        label = this.imageLabel,
        syncState = if (this.syncToFireStoreState != SyncState.Sent.key) {
            this.syncState
        } else {
            this.syncToFireStoreState
        }
    )
}

fun DeploymentImage.toImage(): Image {
    return Image(
        this.id,
        this.imageLabel,
        if (this.imageLabel == "other") ImageType.OTHER else ImageType.NORMAL,
        this.localPath,
        this.remotePath,
        false
    )
}
