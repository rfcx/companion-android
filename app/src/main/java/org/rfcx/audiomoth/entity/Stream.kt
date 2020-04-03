package org.rfcx.audiomoth.entity

import java.io.Serializable


open class Stream(
    val gain: Int,
    val sampleRate: Int,
    val customRecordingPeriod: Boolean,
    val recordingDuration: Int,
    val sleepDuration: Int,
    val recordingPeriodList: ArrayList<String>,
    val durationSelected: String
): Serializable