package org.rfcx.audiomoth.entity.response

import org.rfcx.audiomoth.entity.DeploymentImage

/**
 * Firestore response for getting a deployment image
 */
data class DeploymentImageResponse(
    var deploymentServerId: String = "",
    var remotePath: String = ""
) {
    fun toDeploymentImage(): DeploymentImage {
        return DeploymentImage(
            deploymentServerId = deploymentServerId,
            remotePath = remotePath
        )
    }
}