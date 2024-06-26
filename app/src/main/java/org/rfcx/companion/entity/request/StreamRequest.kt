package org.rfcx.companion.entity.request

import org.rfcx.companion.entity.Stream

data class StreamRequest(
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var project: ProjectRequest? = null,
    var id: String? = null
)

fun Stream.toRequestBody(): StreamRequest {
    return StreamRequest(
        name = this.name,
        latitude = this.latitude,
        longitude = this.longitude,
        altitude = this.altitude,
        project = if (this.project?.name == null) null else this.project?.toRequestBody(),
        id = this.serverId
    )
}
