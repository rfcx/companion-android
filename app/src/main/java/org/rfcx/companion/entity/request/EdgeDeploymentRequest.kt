package org.rfcx.companion.entity.request

import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.EdgeDeployment
import org.rfcx.companion.util.toISO8601Format
import org.rfcx.companion.util.toIsoString
import java.util.*

data class DeploymentRequest(
    var deploymentType: String,
    var deploymentKey: String? = null,
    var deployedAt: String = Date().toISO8601Format(),
    var stream: StreamRequest? = null
)

fun EdgeDeployment.toRequestBody(): DeploymentRequest {
    return DeploymentRequest(
        deploymentType = Device.EDGE.value,
        deployedAt = this.deployedAt.toISO8601Format(),
        stream = if (this.stream?.name == null) null else this.stream?.toRequestBody(),
        deploymentKey = this.deploymentKey
    )
}
