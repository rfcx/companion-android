package org.rfcx.audiomoth.entity

import java.io.Serializable
import java.util.ArrayList

open class Profile(
    val gain: Int = 0,
    val name: String = "",
    val sampleRate: Int = 0,
    val recordingDuration: Int = 0,
    val sleepDuration: Int = 0,
    val recordingPeriodList: ArrayList<String> = arrayListOf(),
    val durationSelected: String = ""
) : Serializable