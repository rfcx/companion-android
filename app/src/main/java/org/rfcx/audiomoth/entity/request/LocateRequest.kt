package org.rfcx.audiomoth.entity.request

import java.util.*
import org.rfcx.audiomoth.entity.Locate
import org.rfcx.audiomoth.entity.LocationGroup

data class LocateRequest(
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var createdAt: Date = Date(),
    var deletedAt: Date? = null,
    var locationGroup: LocationGroup? = null,
    var lastDeploymentServerId: String? = null,
    var lastGuardianDeploymentServerId: String? = null
)

fun Locate.toRequestBody(): LocateRequest {
    return LocateRequest(
        name = this.name,
        latitude = this.latitude,
        longitude = this.longitude,
        createdAt = this.createdAt,
        deletedAt = this.deletedAt,
        locationGroup = this.locationGroup,
        lastDeploymentServerId = this.lastDeploymentServerId,
        lastGuardianDeploymentServerId = this.lastGuardianDeploymentServerId
    )
}
