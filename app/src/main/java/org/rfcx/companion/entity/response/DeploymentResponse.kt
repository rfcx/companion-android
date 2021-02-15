package org.rfcx.companion.entity.response

import org.rfcx.companion.entity.DeploymentLocation
import org.rfcx.companion.entity.DeploymentState
import org.rfcx.companion.entity.EdgeDeployment
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.entity.guardian.GuardianConfiguration
import org.rfcx.companion.entity.guardian.GuardianDeployment
import java.util.*

data class DeploymentResponse(
    var device: String? = null,
    var serverId: String? = null,
    var deploymentKey: String? = null,
    var deployedAt: DateResponse? = DateResponse(),
    var stream: DeploymentLocation? = null,
    var createdAt: DateResponse? = DateResponse(),
    var updatedAt: DateResponse? = null,
    var wifi: String? = null,
    var configuration: GuardianConfiguration? = null,
    var deletedAt: DateResponse? = null
)

fun DeploymentResponse.toGuardianDeployment(): GuardianDeployment {
    return GuardianDeployment(
        serverId = this.serverId,
        deployedAt = this.deployedAt?.seconds?.let { Date(it.times(1000)) } ?: Date(),
        state = DeploymentState.Guardian.ReadyToUpload.key,
        device = this.device,
        wifiName = this.wifi,
        configuration = this.configuration,
        stream = this.stream,
        createdAt = this.createdAt?.seconds?.let { Date(it.times(1000)) } ?: Date(),
        syncState = SyncState.Sent.key,
        updatedAt = this.updatedAt?.seconds?.let { Date(it.times(1000)) }
    )
}


fun DeploymentResponse.toEdgeDeployment(): EdgeDeployment {
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

