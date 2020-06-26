package org.rfcx.audiomoth.entity.request

import org.rfcx.audiomoth.entity.Device
import org.rfcx.audiomoth.entity.guardian.GuardianProfile
import java.util.*

data class GuardianProfileRequest(
    var device: String,
    var name: String = "",
    var sampleRate: Int = 0,
    var bitrate: Int = 0,
    var fileFormat: String = "",
    var duration: Int = 0,
    var createdAt: Date = Date()
)

fun GuardianProfile.toRequestBody(): GuardianProfileRequest {
    return GuardianProfileRequest(
        device = Device.GUARDIAN.value,
        name = this.name,
        sampleRate = this.sampleRate,
        bitrate = this.bitrate,
        fileFormat = this.fileFormat,
        duration = this.duration,
        createdAt = this.createdAt
    )
}
