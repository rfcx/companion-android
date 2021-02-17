package org.rfcx.companion.entity.response

import java.util.*
import org.rfcx.companion.entity.*

/**
 * Firestore response for getting a deployment
 */
data class EdgeDeploymentResponse(
    var serverId: String? = null,
    var deploymentKey: String? = null,
    var deployedAt: DateResponse? = DateResponse(),
    var stream: DeploymentLocation? = null,
    var createdAt: DateResponse? = DateResponse(),
    var updatedAt: DateResponse? = null,
    var deletedAt: DateResponse? = null
)

fun EdgeDeploymentResponse.toEdgeDeployment(): EdgeDeployment {
    return EdgeDeployment(
        deploymentKey = this.deploymentKey,
        serverId = this.serverId,
        deployedAt = this.deployedAt?.seconds?.let { Date(it.times(1000)) } ?: Date(),
        state = DeploymentState.Edge.ReadyToUpload.key,
        stream = this.stream,
        createdAt = this.createdAt?.seconds?.let { Date(it.times(1000)) } ?: Date(),
        syncState = SyncState.Sent.key,
        updatedAt = this.updatedAt?.seconds?.let { Date(it.times(1000)) },
        deletedAt = this.deletedAt?.seconds?.let { Date(it.times(1000)) }
    )
}
