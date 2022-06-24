package org.rfcx.companion.entity.response

import com.google.gson.annotations.SerializedName

data class GuardianClassifierResponse (
    val id: String,
    val name: String,
    val version: String,
    val path: String,
    val type: String,
    val sha1: String,
    @SerializedName("sample_rate")
    val sampleRate: String,
    @SerializedName("input_gain")
    val inputGain: String,
    @SerializedName("window_size")
    val windowSize: String,
    @SerializedName("step_size")
    val stepSize: String,
    val classifications: String,
    @SerializedName("classifications_filter_threshold")
    val classificationsFilterThreshold: String
): FileResponse
