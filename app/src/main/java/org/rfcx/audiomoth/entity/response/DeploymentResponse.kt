package org.rfcx.audiomoth.entity.response

import org.rfcx.audiomoth.entity.Configuration
import org.rfcx.audiomoth.entity.DeploymentLocation
import java.util.*

/**
 * Firestore response for getting a deployment
 */
data class DeploymentResponse(
    var serverId: String? = null,
    var batteryDepletedAt: Date = Date(),
    var batteryLevel: Int = 0,
    var deployedAt: Date = Date(),
    var configuration: Configuration? = null,
    var location: DeploymentLocation? = null,
    var createdAt: Date = Date()
)