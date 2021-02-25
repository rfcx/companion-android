package org.rfcx.companion.entity.response

import org.rfcx.companion.entity.DeploymentImage

/**
 * DeviceAPI response for getting a deployment image
 */
data class DeploymentImageResponse(
    var id: String = "",
    var mimeType: String = ""
) {
    fun toDeploymentImage(): DeploymentImage {
        return DeploymentImage(
            remotePath = "assets/$id"
        )
    }
}
