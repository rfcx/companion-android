package org.rfcx.companion.entity.response

import org.rfcx.companion.entity.*
import java.util.*

/**
 * Firestore response for getting a location
 */
data class StreamResponse(
    var id: String? = null,
    var name: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var altitude: Double? = null,
    var createdAt: Date? = null,
    var project: ProjectResponse? = null
)

fun StreamResponse.toLocate(): Locate {
    return Locate(
        serverId = this.id,
        name = this.name ?: "-",
        latitude = this.latitude ?: 0.0,
        longitude = this.longitude ?: 0.0,
        createdAt = this.createdAt ?: Date(),
        locationGroup = this.project?.toLocationGroup(),
        syncState = SyncState.Sent.key
    )
}

fun StreamResponse.toDeploymentLocation(): DeploymentLocation {
    return DeploymentLocation(
        coreId = this.id,
        name = this.name ?: "-",
        latitude = this.latitude ?: 0.0,
        longitude = this.longitude ?: 0.0,
        project = this.project?.toLocationGroup()
    )
}

fun DeploymentLocation.toLocationResponse(): StreamResponse {
    return StreamResponse(
        id = this.coreId,
        name = this.name,
        latitude = this.latitude,
        longitude = this.longitude,
        createdAt = Date(),
        project = this.project?.toLocationGroupsResponse()
    )
}
