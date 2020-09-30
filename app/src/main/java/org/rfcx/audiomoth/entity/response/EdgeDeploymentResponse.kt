package org.rfcx.audiomoth.entity.response

import java.util.*
import org.rfcx.audiomoth.entity.*

/**
 * Firestore response for getting a deployment
 */
data class EdgeDeploymentResponse(
    var serverId: String? = null,
    var deploymentId: String? = null,
    var deployedAt: Date? = Date(),
    var location: DeploymentLocation? = null,
    var createdAt: Date? = Date(),
    var updatedAt: Date? = null,
    var deletedAt: Date? = null
)

fun EdgeDeploymentResponse.toEdgeDeployment(): EdgeDeployment {
    return EdgeDeployment(
        deploymentId = this.deploymentId,
        serverId = this.serverId,
        deployedAt = this.deployedAt ?: Date(),
        state = DeploymentState.Edge.ReadyToUpload.key,
        location = this.location,
        createdAt = this.createdAt ?: Date(),
        syncState = SyncState.Sent.key,
        updatedAt = this.updatedAt,
        deletedAt = this.deletedAt
    )
}
