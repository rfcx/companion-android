package org.rfcx.audiomoth.entity.request

import java.util.*
import org.rfcx.audiomoth.entity.DeploymentLocation
import org.rfcx.audiomoth.entity.Device
import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration
import org.rfcx.audiomoth.entity.guardian.GuardianDeployment

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
