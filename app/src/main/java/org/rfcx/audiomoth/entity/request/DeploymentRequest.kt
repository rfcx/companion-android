package org.rfcx.audiomoth.entity.request

import org.rfcx.audiomoth.entity.Configuration
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.DeploymentLocation
import java.util.*

data class DeploymentRequest(
    var batteryDepletedAt: Date = Date(),
    var batteryLevel: Int = 0,
    var deployedAt: Date = Date(),
    var configuration: Configuration? = null,
    var location: DeploymentLocation? = null,
    var createdAt: Date = Date()
)

fun Deployment.toRequestBody(): DeploymentRequest {
    return DeploymentRequest(
        batteryDepletedAt = this.batteryDepletedAt,
        batteryLevel = this.batteryLevel,
        deployedAt = this.deployedAt,
        configuration = this.configuration,
        location = this.location,
        createdAt = this.createdAt
    )
}