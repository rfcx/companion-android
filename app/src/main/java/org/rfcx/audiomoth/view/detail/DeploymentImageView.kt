package org.rfcx.audiomoth.view.detail

import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.DeploymentImage
import org.rfcx.audiomoth.entity.SyncState

data class DeploymentImageView(
    val id: Int,
    val localPath: String,
    val remotePath: String?,
    var syncState: Int = 0 // syncToFireStoreState
) {
    val syncImage = when (syncState) {
        SyncState.Unsent.key -> R.drawable.ic_cloud_queue_24px
        SyncState.Sending.key -> R.drawable.ic_cloud_upload_24px
        else -> R.drawable.ic_cloud_done_24px
    }
}

fun DeploymentImage.toDeploymentImageView(): DeploymentImageView {
    return DeploymentImageView(
        id = this.id,
        localPath = this.localPath,
        remotePath = this.remotePath,
        syncState = this.syncToFireStoreState
    )
}
