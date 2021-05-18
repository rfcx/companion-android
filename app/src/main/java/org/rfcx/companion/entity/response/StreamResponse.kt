package org.rfcx.companion.entity.response

import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.GuardianConfiguration
import org.rfcx.companion.entity.guardian.GuardianDeployment
import java.util.*

data class StreamResponse(
    var id: String? = null,
    var name: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var altitude: Double? = null,
    var createdAt: Date? = null,
    var updatedAt: Date? = null,
    var project: ProjectResponse? = null,
    var deployment: DeploymentResponse? = null
)

fun StreamResponse.convertToDeploymentResponse(): DeploymentResponse {
    val tempDeployment = this.deployment
    tempDeployment!!.stream = this
    return tempDeployment
}

fun StreamResponse.toLocate(): Locate {
    return Locate(
        serverId = this.id,
        name = this.name ?: "-",
        latitude = this.latitude ?: 0.0,
        longitude = this.longitude ?: 0.0,
        altitude = this.altitude ?: 0.0,
        createdAt = this.createdAt ?: Date(),
        updatedAt = this.updatedAt,
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
        altitude = this.altitude ?: 0.0,
        project = this.project?.toLocationGroup()
    )
}

fun DeploymentLocation.toLocationResponse(): StreamResponse {
    return StreamResponse(
        id = this.coreId,
        name = this.name,
        latitude = this.latitude,
        longitude = this.longitude,
        altitude = this.altitude,
        createdAt = Date(),
        project = this.project?.toLocationGroupsResponse()
    )
}
