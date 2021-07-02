package org.rfcx.companion.entity.response

import org.rfcx.companion.entity.DeploymentState
import org.rfcx.companion.entity.Deployment
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.entity.guardian.GuardianConfiguration
import org.rfcx.companion.entity.guardian.GuardianDeployment
import java.util.*

data class DeploymentResponse(
    var id: String? = null,
    var deploymentType: String? = null,
    var deployedAt: Date? = null,
    var stream: StreamResponse? = null,
    var createdAt: Date? = null,
    var updatedAt: Date? = null,
    var wifi: String? = null,
    var configuration: GuardianConfiguration? = null,
    var deletedAt: Date? = null
)

fun DeploymentResponse.toGuardianDeployment(): GuardianDeployment {
    return GuardianDeployment(
        serverId = this.id,
        deployedAt = this.deployedAt ?: Date(),
        state = DeploymentState.Guardian.ReadyToUpload.key,
        device = this.deploymentType,
        wifiName = this.wifi ?: "",
        configuration = this.configuration ?: GuardianConfiguration(),
        stream = this.stream?.toDeploymentLocation(),
        createdAt = this.createdAt ?: Date(),
        syncState = SyncState.Sent.key,
        updatedAt = this.updatedAt,
        isActive = true
    )
}


fun DeploymentResponse.toEdgeDeployment(): Deployment {
    return Deployment(
        deploymentKey = this.id,
        serverId = this.id,
        deployedAt = this.deployedAt ?: Date(),
        state = DeploymentState.Edge.ReadyToUpload.key,
        stream = this.stream?.toDeploymentLocation(),
        createdAt = this.createdAt ?: Date(),
        syncState = SyncState.Sent.key,
        updatedAt = this.updatedAt,
        deletedAt = this.deletedAt
    )
}

