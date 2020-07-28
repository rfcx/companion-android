package org.rfcx.audiomoth.entity.request

import java.util.*
import org.rfcx.audiomoth.entity.Profile

data class ProfileRequest(
    var name: String = "",
    var gain: Int = 0,
    var sampleRate: Int = 0,
    var recordingDuration: Int = 0,
    var sleepDuration: Int = 0,
    var recordingPeriodList: ArrayList<String> = arrayListOf(),
    var durationSelected: String = "",
    var createdAt: Date = Date()
)

fun Profile.toRequestBody(): ProfileRequest {
    return ProfileRequest(
        name = this.name,
        gain = this.gain,
        sampleRate = this.sampleRate,
        recordingDuration = this.recordingDuration,
        sleepDuration = this.sleepDuration,
        recordingPeriodList = this.recordingPeriodList.mapTo(arrayListOf(), { it }),
        durationSelected = this.durationSelected,
        createdAt = this.createdAt
    )
}
