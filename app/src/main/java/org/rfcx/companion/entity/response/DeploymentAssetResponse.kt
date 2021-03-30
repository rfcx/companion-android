package org.rfcx.companion.entity.response

import org.rfcx.companion.entity.DeploymentImage
import org.rfcx.companion.entity.TrackingFile

/**
 * DeviceAPI response for getting a deployment asset
 */
data class DeploymentAssetResponse(
    var id: String = "",
    var mimeType: String = ""
) {
    fun toDeploymentImage(): DeploymentImage {
        return DeploymentImage(
            remotePath = "assets/$id"
        )
    }

    fun toDeploymentTrack(): TrackingFile {
        return TrackingFile(
            remotePath = "assets/$id"
        )
    }
}
