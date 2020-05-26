package org.rfcx.audiomoth.entity

import org.rfcx.audiomoth.view.configure.ConfigureFragment.Companion.RECOMMENDED
import java.util.*

data class Configuration(
    val gain: Int = 0,
    val sampleRate: Int = 0,
    val recordingDuration: Int = 0,
    val sleepDuration: Int = 0,
    val recordingPeriodList: ArrayList<String> = arrayListOf(),
    val durationSelected: String = ""
) {
    companion object {
        fun default() = Configuration(
            gain = 3,
            sampleRate = 8,
            recordingDuration = 5,
            sleepDuration = 10,
            recordingPeriodList = arrayListOf(),
            durationSelected = RECOMMENDED
        )
    }
}