package org.rfcx.audiomoth.entity.response

import io.realm.RealmList
import java.util.*
import org.rfcx.audiomoth.entity.*
import org.rfcx.audiomoth.util.EdgeConfigure

/**
 * Firestore response for getting a deployment
 */
data class EdgeDeploymentResponse(
    var serverId: String? = null,
    var deploymentId: String? = null,
    var batteryDepletedAt: Date? = Date(),
    var batteryLevel: Int? = 0,
    var deployedAt: Date? = Date(),
    var configuration: ConfigurationResponse? = null,
    var location: DeploymentLocation? = null,
    var createdAt: Date? = Date(),
    var updatedAt: Date? = null,
    var deletedAt: Date? = null
)

data class ConfigurationResponse(
    var gain: Int? = null,
    var sampleRate: Int? = null,
    var recordingDuration: Int? = null,
    var sleepDuration: Int? = null,
    var recordingPeriodList: ArrayList<String>? = arrayListOf(),
    var durationSelected: String? = null
) {
    fun toConfiguration(): Configuration {
        val realmList = recordingPeriodList?.mapTo(RealmList(), { it })
        return Configuration(
            gain ?: EdgeConfigure.GAIN_DEFAULT,
            sampleRate ?: EdgeConfigure.SAMPLE_RATE_DEFAULT,
            recordingDuration ?: EdgeConfigure.RECORDING_DURATION_DEFAULT,
            sleepDuration ?: EdgeConfigure.SLEEP_DURATION_DEFAULT,
            realmList ?: RealmList(),
            durationSelected ?: EdgeConfigure.DURATION_SELECTED_DEFAULT
        )
    }
}

fun EdgeDeploymentResponse.toDeployment(): EdgeDeployment {
    return EdgeDeployment(
        deploymentId = this.deploymentId,
        serverId = this.serverId,
        batteryDepletedAt = this.batteryDepletedAt ?: Date(),
        deployedAt = this.deployedAt ?: Date(),
        batteryLevel = this.batteryLevel ?: 0,
        state = DeploymentState.Edge.ReadyToUpload.key,
        configuration = this.configuration?.toConfiguration(),
        location = this.location,
        createdAt = this.createdAt ?: Date(),
        syncState = SyncState.Sent.key,
        updatedAt = this.updatedAt,
        deletedAt = this.deletedAt
    )
}
