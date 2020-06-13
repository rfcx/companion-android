package org.rfcx.audiomoth.entity.request

import org.rfcx.audiomoth.entity.DeploymentImage

data class ImageRequest(
    var deploymentServerId: String? = null,
    var remotePath: String? = null
)

fun DeploymentImage.toRequestBody(): ImageRequest {
    return ImageRequest(
        deploymentServerId = this.deploymentServerId,
        remotePath = this.remotePath
    )
}