package org.rfcx.audiomoth.entity.request

import java.util.*
import org.rfcx.audiomoth.entity.Locate

data class LocateRequest(
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var createdAt: Date = Date(),
    var lastDeploymentServerId: String? = null,
    var lastGuardianDeploymentServerId: String? = null
)

fun Locate.toRequestBody(): LocateRequest {
    return LocateRequest(
        name = this.name,
        latitude = this.latitude,
        longitude = this.longitude,
        createdAt = this.createdAt,
        lastDeploymentServerId = this.lastDeploymentServerId,
        lastGuardianDeploymentServerId = this.lastGuardianDeploymentServerId
    )
}
