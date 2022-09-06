package org.rfcx.companion.view.map

import java.util.*

sealed class MapMarker {
    data class SiteMarker(
        val id: Int,
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val projectName: String?,
        val createdAt: Date,
        val pin: String
    ) : MapMarker()

    data class DeploymentMarker(
        val id: Int,
        val locationName: String,
        val longitude: Double,
        val latitude: Double,
        val pin: String,
        val description: String,
        val device: String,
        val projectName: String?,
        val deploymentKey: String,
        val createdAt: Date,
        val deploymentAt: Date,
        val updatedAt: Date?
    ) : MapMarker()
}
