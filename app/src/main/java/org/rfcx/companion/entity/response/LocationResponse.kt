package org.rfcx.companion.entity.response

import org.rfcx.companion.entity.*
import java.util.*

/**
 * Firestore response for getting a location
 */
data class LocationResponse(
    var serverId: String? = null,
    var name: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var altitude: Double? = null,
    var createdAt: Date? = null,
    var deletedAt: Date? = null,
    var locationGroup: LocationGroup? = null,
    var lastDeploymentServerId: String? = null,
    var lastGuardianDeploymentServerId: String? = null
)

fun LocationResponse.toLocate(): Locate {
    return Locate(
        serverId = this.serverId,
        name = this.name ?: "-",
        latitude = this.latitude ?: 0.0,
        longitude = this.longitude ?: 0.0,
        altitude = this.altitude ?: 0.0,
        createdAt = this.createdAt ?: Date(),
        deletedAt = this.deletedAt,
        locationGroup = this.locationGroup,
        lastDeploymentServerId = this.lastDeploymentServerId,
        lastGuardianDeploymentServerId = this.lastGuardianDeploymentServerId,
        syncState = SyncState.Sent.key
    )
}

fun DeploymentLocation.toLocationResponse(): LocationResponse {
    return LocationResponse(
        serverId = this.coreId,
        name = this.name,
        latitude = this.latitude,
        longitude = this.longitude,
        altitude = this.altitude,
        createdAt = Date(),
        deletedAt = null,
        locationGroup = this.project,
        lastDeploymentServerId = null,
        lastGuardianDeploymentServerId = null
    )
}
