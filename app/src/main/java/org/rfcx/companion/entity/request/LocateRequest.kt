package org.rfcx.companion.entity.request

import java.util.*
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.LocationGroup

data class LocateRequest(
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
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
        altitude = this.altitude,
        createdAt = this.createdAt,
        deletedAt = this.deletedAt,
        locationGroup = this.locationGroup,
        lastDeploymentServerId = this.lastDeploymentServerId,
        lastGuardianDeploymentServerId = this.lastGuardianDeploymentServerId
    )
}
