package org.rfcx.audiomoth.entity.guardian

import com.google.gson.annotations.SerializedName
import io.realm.RealmModel
import io.realm.annotations.RealmClass

@RealmClass
open class GuardianConfiguration(
    @SerializedName("sample_rate")
    var sampleRate: Int = 14,
    var bitrate: Int = 14,
    @SerializedName("file_format")
    var fileFormat: String = "opus",
    var duration: Int = 90
) : RealmModel

fun GuardianConfiguration.toListForGuardian(): List<String> {
    return listOf(
        "audio_sample_rate|$sampleRate",
        "audio_encode_bitrate|$bitrate",
        "audio_encode_codec|$fileFormat",
        "audio_cycle_duration|$duration"
    )
}

fun GuardianConfiguration.toReadableFormat(): GuardianConfiguration {
    return GuardianConfiguration(
        sampleRate = sampleRate / 1000,
        bitrate = bitrate / 1024,
        fileFormat = fileFormat,
        duration = duration
    )
}
