package org.rfcx.audiomoth.entity.socket

import com.google.gson.annotations.SerializedName

data class ConfigurationResponse(
    @SerializedName("sample_rate")
    val sampleRate: Int,
    val bitrate: Int,
    @SerializedName("file_format")
    val fileFormat: String,
    val duration: Int
) : SocketResposne
