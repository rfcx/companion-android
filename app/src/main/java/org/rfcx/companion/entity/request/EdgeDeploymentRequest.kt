package org.rfcx.companion.entity.request

import java.util.*
import org.rfcx.companion.entity.DeploymentLocation
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.EdgeDeployment

data class DeploymentRequest(
    var device: String,
    var deploymentKey: String? = null,
    var deployedAt: Date = Date(),
    var stream: StreamRequest? = null,
    var createdAt: Date = Date(),
    var updatedAt: Date? = null,
    var deletedAt: Date? = null
)

fun EdgeDeployment.toRequestBody(): DeploymentRequest {
    return DeploymentRequest(
        device = Device.EDGE.value,
        deployedAt = this.deployedAt,
        stream = if (this.stream?.name == null) null else this.stream?.toRequestBody(),
        createdAt = this.createdAt,
        deploymentKey = this.deploymentKey,
        updatedAt = this.updatedAt,
        deletedAt = this.deletedAt
    )
}
