package org.rfcx.audiomoth.view.map

import java.util.*

data class DeploymentMarker(
    val id: Int,
    val locationName: String,
    val longitude: Double,
    val latitude: Double,
    val pin: String,
    val description: String,
    val device: String,
    val createdAt: Date
)
