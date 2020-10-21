package org.rfcx.companion.entity.response

import org.rfcx.companion.entity.DeploymentImage

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
