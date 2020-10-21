package org.rfcx.companion.entity.response

import java.util.*
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.LocationGroup
import org.rfcx.companion.entity.SyncState

/**
 * Firestore response for getting a location
 */
data class LocationResponse(
    var serverId: String? = null,
    var name: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
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
        createdAt = this.createdAt ?: Date(),
        deletedAt = this.deletedAt,
        locationGroup = this.locationGroup,
        lastDeploymentServerId = this.lastDeploymentServerId,
        lastGuardianDeploymentServerId = this.lastGuardianDeploymentServerId,
        syncState = SyncState.Sent.key
    )
}
