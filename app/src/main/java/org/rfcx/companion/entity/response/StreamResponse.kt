package org.rfcx.companion.entity.response

import org.rfcx.companion.entity.*
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

fun StreamResponse.toStream(): Stream {
    return Stream(
        serverId = this.id,
        name = this.name ?: "-",
        latitude = this.latitude ?: 0.0,
        longitude = this.longitude ?: 0.0,
        altitude = this.altitude ?: 0.0,
        createdAt = this.createdAt ?: Date(),
        updatedAt = this.updatedAt,
        syncState = SyncState.Sent.key
    )
}
