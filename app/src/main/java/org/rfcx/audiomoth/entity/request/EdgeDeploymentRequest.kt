package org.rfcx.audiomoth.entity.request

import java.util.*
import org.rfcx.audiomoth.entity.EdgeConfiguration
import org.rfcx.audiomoth.entity.EdgeDeployment
import org.rfcx.audiomoth.entity.DeploymentLocation
import org.rfcx.audiomoth.entity.Device

data class DeploymentRequest(
    var device: String,
    var deploymentId: String? = null,
    var batteryDepletedAt: Date = Date(),
    var batteryLevel: Int = 0,
    var deployedAt: Date = Date(),
    var configuration: EdgeConfiguration? = null,
    var location: DeploymentLocation? = null,
    var createdAt: Date = Date(),
    var updatedAt: Date? = null,
    var deletedAt: Date? = null
)

fun EdgeDeployment.toRequestBody(): DeploymentRequest {
    return DeploymentRequest(
        device = Device.EDGE.value,
        batteryDepletedAt = this.batteryDepletedAt,
        batteryLevel = this.batteryLevel,
        deployedAt = this.deployedAt,
        configuration = this.configuration,
        location = this.location,
        createdAt = this.createdAt,
        deploymentId = this.deploymentId,
        updatedAt = this.updatedAt,
        deletedAt = this.deletedAt
    )
}
