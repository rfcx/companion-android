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
    var fileFormat: String = "OPUS",
    var duration: Int = 90
) : RealmModel
