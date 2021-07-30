package org.rfcx.companion.entity.guardian

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.realm.RealmModel
import io.realm.annotations.RealmClass
import org.rfcx.companion.entity.request.GuardianConfigurationRequest
import java.io.Serializable

@RealmClass
open class GuardianConfiguration(
    @SerializedName("sample_rate")
    var sampleRate: Int = 24000,
    var bitrate: Int = 28672,
    @SerializedName("file_format")
    var fileFormat: String = "opus",
    var duration: Int = 90
) : RealmModel, Serializable

fun GuardianConfiguration.toListForGuardian(): String {
    val json = JsonObject().apply {
        addProperty("audio_sample_rate", sampleRate)
        addProperty("audio_encode_bitrate", bitrate)
        addProperty("audio_encode_codec", fileFormat)
        addProperty("audio_cycle_duration", duration)
    }
    return Gson().toJson(json)
}

fun GuardianConfiguration.toReadableFormat(): GuardianConfiguration {
    return GuardianConfiguration(
        sampleRate = sampleRate / 1000,
        bitrate = bitrate / 1024,
        fileFormat = fileFormat,
        duration = duration
    )
}

fun GuardianConfiguration.toRequestBody(): GuardianConfigurationRequest {
    return GuardianConfigurationRequest(
        sampleRate = this.sampleRate,
        bitrate = this.bitrate,
        duration = this.duration,
        fileFormat = this.fileFormat
    )
}
