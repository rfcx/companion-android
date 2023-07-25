package org.rfcx.companion.entity

import org.rfcx.companion.view.map.MapMarker
import java.util.Date

data class InfoWindowMarker(
    val id: Int,
    val locationName: String,
    val longitude: Double,
    val latitude: Double,
    val pin: String,
    val device: String?,
    val projectName: String?,
    val deploymentKey: String?,
    val createdAt: Date,
    val deploymentAt: Date?,
    val updatedAt: Date?,
    val isDeployment: Boolean
)

fun MapMarker.SiteMarker.toInfoWindowMarker(): InfoWindowMarker {
    return InfoWindowMarker(
        this.id,
        this.name,
        this.longitude,
        this.latitude,
        this.pin,
        null,
        this.projectName,
        null,
        this.createdAt,
        null,
        null,
        false
    )
}

fun MapMarker.DeploymentMarker.toInfoWindowMarker(): InfoWindowMarker {
    return InfoWindowMarker(
        this.id,
        this.locationName,
        this.longitude,
        this.latitude,
        this.pin,
        this.device,
        this.projectName,
        this.deploymentKey,
        this.createdAt,
        this.deploymentAt,
        this.updatedAt,
        true
    )
}
