package org.rfcx.audiomoth.entity.request

import org.rfcx.audiomoth.entity.Locate
import java.util.*

data class LocateRequest(
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var createdAt: Date = Date(),
    var lastDeployment: Int = 0,
    var lastDeploymentServerId: String? = null
)

fun Locate.toRequestBody(): LocateRequest {
    return LocateRequest(
        name = this.name,
        latitude = this.latitude,
        longitude = this.longitude,
        createdAt = this.createdAt,
        lastDeployment = this.lastDeployment
    )
}