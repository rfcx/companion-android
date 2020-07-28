package org.rfcx.audiomoth.entity.response

import java.util.*
import org.rfcx.audiomoth.entity.DeploymentLocation
import org.rfcx.audiomoth.entity.DeploymentState
import org.rfcx.audiomoth.entity.SyncState
import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration
import org.rfcx.audiomoth.entity.guardian.GuardianDeployment

data class GuardianDeploymentResponse(
    var device: String? = null,
    var serverId: String? = null,
    var deployedAt: Date? = Date(),
    var wifi: String? = null,
    var configuration: GuardianConfiguration? = null,
    var location: DeploymentLocation? = null,
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
        syncState = SyncState.Sent.key
    )
}
