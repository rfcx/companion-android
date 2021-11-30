package org.rfcx.companion.entity.guardian

import com.google.gson.JsonObject
import org.rfcx.companion.util.prefs.PrefsUtils

data class GuardianConfiguration(
    var sampleRate: Int = 12000,
    var bitrate: Int = 16384,
    var fileFormat: String = "opus",
    var duration: Int = 90
)

fun GuardianConfiguration.toListForGuardian(): String {
    val json = JsonObject().apply {
        addProperty(PrefsUtils.audioSampleRate, sampleRate)
        addProperty(PrefsUtils.audioBitrate, bitrate)
        addProperty(PrefsUtils.audioCodec, fileFormat)
        addProperty(PrefsUtils.audioDuration, duration)
    }
    return json.toString()
}

