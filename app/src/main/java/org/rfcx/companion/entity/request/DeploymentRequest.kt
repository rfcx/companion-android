package org.rfcx.companion.entity.request

import com.google.gson.annotations.SerializedName
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.guardian.toRequestBody
import org.rfcx.companion.util.toISO8601Format
import java.util.*

data class DeploymentRequest(
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

fun Deployment.toRequestBody(): DeploymentRequest {
    return DeploymentRequest(
        deploymentKey = this.deploymentKey,
        deploymentType = this.device ?: "",
        deployedAt = this.deployedAt.toISO8601Format(),
        wifi = this.wifiName,
        configuration = this.configuration?.toRequestBody(),
        stream = if (this.stream?.name == null) null else this.stream?.toRequestBody()
    )
}
