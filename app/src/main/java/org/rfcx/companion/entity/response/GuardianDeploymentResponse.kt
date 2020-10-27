package org.rfcx.companion.entity.response

import java.util.*
import org.rfcx.companion.entity.DeploymentLocation
import org.rfcx.companion.entity.DeploymentState
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.entity.guardian.GuardianConfiguration
import org.rfcx.companion.entity.guardian.GuardianDeployment

data class GuardianDeploymentResponse(
    var device: String? = null,
    var serverId: String? = null,
    var deployedAt: Date? = Date(),
    var wifi: String? = null,
    var configuration: GuardianConfiguration? = null,
    var location: DeploymentLocation? = null,
    var updatedAt: Date? = null,
    var createdAt: Date? = Date()
)

fun GuardianDeploymentResponse.toGuardianDeployment(): GuardianDeployment {
    return GuardianDeployment(
        serverId = this.serverId,
        deployedAt = this.deployedAt ?: Date(),
        state = DeploymentState.Guardian.ReadyToUpload.key,
        device = this.device,
        wifiName = this.wifi,
        configuration = this.configuration,
        location = this.location,
        createdAt = this.createdAt ?: Date(),
        updatedAt = this.updatedAt,
        syncState = SyncState.Sent.key
    )
}
