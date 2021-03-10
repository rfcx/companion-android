package org.rfcx.companion.entity.request

import java.util.*
import org.rfcx.companion.entity.DeploymentLocation
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.guardian.GuardianConfiguration
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.util.randomDeploymentId
import org.rfcx.companion.util.toISO8601Format

data class GuardianDeploymentRequest(
    var deploymentKey: String,
    var deploymentType: String,
    var deployedAt: String = Date().toISO8601Format(),
    var wifi: String,
    var configuration: GuardianConfiguration? = null,
    var stream: StreamRequest? = null
)

fun GuardianDeployment.toRequestBody(): GuardianDeploymentRequest {
    return GuardianDeploymentRequest(
        deploymentKey = randomDeploymentId(),
        deploymentType = Device.GUARDIAN.value,
        deployedAt = this.deployedAt.toISO8601Format(),
        wifi = this.wifiName!!,
        configuration = this.configuration,
        stream = if (this.stream?.name == null) null else this.stream?.toRequestBody()
    )
}
