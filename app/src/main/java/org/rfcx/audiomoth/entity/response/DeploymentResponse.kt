package org.rfcx.audiomoth.entity.response

import io.realm.RealmList
import org.rfcx.audiomoth.entity.*
import java.util.*

/**
 * Firestore response for getting a deployment
 */
data class DeploymentResponse(
    var serverId: String? = null,
    var batteryDepletedAt: Date? = Date(),
    var batteryLevel: Int? = 0,
    var deployedAt: Date? = Date(),
    var configuration: ConfigurationResponse? = null,
    var location: DeploymentLocation? = null,
    var createdAt: Date? = Date()
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
            gain ?: 3,
            sampleRate ?: 8,
            recordingDuration ?: 5,
            sleepDuration ?: 10,
            realmList ?: RealmList<String>(),
            durationSelected ?: "RECOMMENDED"
        )
    }
}


fun DeploymentResponse.toDeployment(): Deployment {
    return Deployment(
        serverId = this.serverId,
        batteryDepletedAt = this.batteryDepletedAt ?: Date(),
        deployedAt = this.deployedAt ?: Date(),
        batteryLevel = this.batteryLevel ?: 0,
        state = DeploymentState.AudioMoth.ReadyToUpload.key,
        configuration = this.configuration?.toConfiguration(),
        location = this.location,
        createdAt = this.createdAt ?: Date(),
        syncState = SyncState.Sent.key
    )
}