package org.rfcx.audiomoth.entity.response

import org.rfcx.audiomoth.entity.DeploymentLocation
import org.rfcx.audiomoth.entity.DeploymentState
import org.rfcx.audiomoth.entity.SyncState
import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration
import org.rfcx.audiomoth.entity.guardian.GuardianDeployment
import java.util.*

data class GuardianDeploymentResponse(
    var serverId: String? = null,
    var deployedAt: Date? = Date(),
    var configuration: GuardianConfiguration? = null,
    var location: DeploymentLocation? = null,
    var createdAt: Date? = Date()
)

fun GuardianDeploymentResponse.toGuardianDeployment(): GuardianDeployment {
    return GuardianDeployment(
        serverId = this.serverId,
        deployedAt = this.deployedAt ?: Date(),
        state = DeploymentState.Guardian.ReadyToUpload.key,
        configuration = this.configuration,
        location = this.location,
        createdAt = this.createdAt ?: Date(),
        syncState = SyncState.Sent.key
    )
}
