package org.rfcx.companion.entity.request

import com.google.gson.annotations.SerializedName
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.entity.guardian.toRequestBody
import org.rfcx.companion.util.randomDeploymentId
import org.rfcx.companion.util.toISO8601Format
import java.util.*

data class GuardianDeploymentRequest(
    var deploymentKey: String,
    var deploymentType: String,
    var deployedAt: String = Date().toISO8601Format(),
    var wifi: String? = null,
    var configuration: GuardianConfigurationRequest? = null,
    var stream: StreamRequest? = null
)

data class GuardianConfigurationRequest(
    @SerializedName("sample_rate")
    var sampleRate: Int = 24000,
    var bitrate: Int = 28672,
    @SerializedName("file_format")
    var fileFormat: String = "opus",
    var duration: Int = 90
)

fun GuardianDeployment.toRequestBody(): GuardianDeploymentRequest {
    return GuardianDeploymentRequest(
        deploymentKey = randomDeploymentId(),
        deploymentType = Device.GUARDIAN.value,
        deployedAt = this.deployedAt.toISO8601Format(),
        wifi = this.wifiName,
        configuration = this.configuration?.toRequestBody(),
        stream = if (this.stream?.name == null) null else this.stream?.toRequestBody()
    )
}
