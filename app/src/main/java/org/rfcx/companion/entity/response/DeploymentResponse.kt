package org.rfcx.companion.entity.response

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.rfcx.companion.entity.DeploymentState
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.entity.guardian.Deployment
import java.util.*

data class DeploymentResponse(
    var id: String? = null,
    var deploymentType: String? = null,
    var deployedAt: Date? = null,
    var stream: StreamResponse? = null,
    var createdAt: Date? = null,
    var updatedAt: Date? = null,
    var deletedAt: Date? = null,
    var deviceParameters: JsonElement? = null
)

fun DeploymentResponse.isGuardian(): Boolean {
    return this.deploymentType == Device.GUARDIAN.value
}

fun DeploymentResponse.toDeployment(): Deployment {
    return Deployment(
        serverId = this.id,
        deploymentKey = this.id ?: "",
        deployedAt = this.deployedAt ?: Date(),
        state = if (this.isGuardian()) DeploymentState.Guardian.ReadyToUpload.key else DeploymentState.AudioMoth.ReadyToUpload.key,
        device = this.deploymentType,
        stream = this.stream?.toDeploymentLocation(),
        createdAt = this.createdAt ?: Date(),
        syncState = SyncState.Sent.key,
        updatedAt = this.updatedAt,
        deletedAt = this.deletedAt,
        isActive = true,
        deviceParameters = if (this.deviceParameters is JsonNull) null else Gson().toJson(this.deviceParameters)
    )
}
