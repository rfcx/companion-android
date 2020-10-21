package org.rfcx.companion.entity.request

import java.util.*
import org.rfcx.companion.entity.DeploymentLocation
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.EdgeDeployment

data class DeploymentRequest(
    var device: String,
    var deploymentId: String? = null,
    var deployedAt: Date = Date(),
    var location: DeploymentLocation? = null,
    var createdAt: Date = Date(),
    var updatedAt: Date? = null,
    var deletedAt: Date? = null
)

fun EdgeDeployment.toRequestBody(): DeploymentRequest {
    return DeploymentRequest(
        device = Device.EDGE.value,
        deployedAt = this.deployedAt,
        location = this.location,
        createdAt = this.createdAt,
        deploymentId = this.deploymentId,
        updatedAt = this.updatedAt,
        deletedAt = this.deletedAt
    )
}
