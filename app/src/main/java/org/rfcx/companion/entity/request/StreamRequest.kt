package org.rfcx.companion.entity.request

import org.rfcx.companion.entity.DeploymentLocation

data class StreamRequest(
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var project: ProjectRequest? = null,
    var coreId: String? = null
)

fun DeploymentLocation.toRequestBody(): StreamRequest {
    return StreamRequest(
        name = this.name,
        latitude = this.latitude,
        longitude = this.longitude,
        altitude = this.altitude,
        project = if (this.project?.name == null) null else this.project?.toRequestBody(),
        coreId = this.coreId
    )
}
