package org.rfcx.companion.entity.guardian

import com.google.gson.JsonObject
import org.rfcx.companion.util.prefs.PrefsUtils

data class GuardianConfiguration(
    var sampleRate: Int = 12000,
    var bitrate: Int = 16384,
    var fileFormat: String = "opus",
    var duration: Int = 90,
    var enableSampling: Boolean = false,
    var sampling: String = "1:2",
    var schedule: String = "23:55-23:56,23:57-23:59"
)

fun GuardianConfiguration.toListForGuardian(): String {
    val json = JsonObject().apply {
        addProperty(PrefsUtils.audioSampleRate, sampleRate)
        addProperty(PrefsUtils.audioCastSampleRate, sampleRate)
        addProperty(PrefsUtils.audioBitrate, bitrate)
        addProperty(PrefsUtils.audioCodec, fileFormat)
        addProperty(PrefsUtils.audioDuration, duration)
        addProperty(PrefsUtils.enableSampling, enableSampling)
        addProperty(PrefsUtils.sampling, sampling)
        addProperty(PrefsUtils.schedule, schedule)
    }
    return json.toString()
}
