package org.rfcx.companion.entity.request

import java.util.*
import org.rfcx.companion.entity.DeploymentLocation
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.guardian.GuardianConfiguration
import org.rfcx.companion.entity.guardian.GuardianDeployment

data class GuardianDeploymentRequest(
    var device: String,
    var deployedAt: Date = Date(),
    var wifi: String,
    var configuration: GuardianConfiguration? = null,
    var location: DeploymentLocation? = null,
    var createdAt: Date = Date()
)

fun GuardianDeployment.toRequestBody(): GuardianDeploymentRequest {
    return GuardianDeploymentRequest(
        device = Device.GUARDIAN.value,
        deployedAt = this.deployedAt,
        wifi = this.wifiName!!,
        configuration = this.configuration,
        location = this.location,
        createdAt = this.createdAt
    )
}
