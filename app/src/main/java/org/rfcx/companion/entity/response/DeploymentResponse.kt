package org.rfcx.companion.entity.response

import org.rfcx.companion.entity.DeploymentState
import org.rfcx.companion.entity.Device
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

fun DeploymentResponse.isGuardian(): Boolean {
    return this.deploymentType == Device.GUARDIAN.value
}

fun DeploymentResponse.toGuardianDeployment(): GuardianDeployment {
    return GuardianDeployment(
        serverId = this.id,
        deploymentKey = this.id ?: "",
        deployedAt = this.deployedAt ?: Date(),
        state = if (this.isGuardian()) DeploymentState.Guardian.ReadyToUpload.key else DeploymentState.Edge.ReadyToUpload.key,
        device = this.deploymentType,
        wifiName = this.wifi ?: "",
        configuration = this.configuration ?: GuardianConfiguration(),
        stream = this.stream?.toDeploymentLocation(),
        createdAt = this.createdAt ?: Date(),
        syncState = SyncState.Sent.key,
        updatedAt = this.updatedAt,
        deletedAt = this.deletedAt,
        isActive = true
    )
}
