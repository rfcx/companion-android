package org.rfcx.audiomoth.entity

import org.rfcx.audiomoth.view.configure.ConfigureFragment
import java.util.*

data class Profile(
    val gain: Int = 0,
    val name: String = "",
    val sampleRate: Int = 0,
    val recordingDuration: Int = 0,
    val sleepDuration: Int = 0,
    val recordingPeriodList: ArrayList<String> = arrayListOf(),
    val durationSelected: String = ""
) {
    companion object {
        fun default() = Profile(
            gain = 3,
            name = "",
            sampleRate = 8,
            recordingDuration = 5,
            sleepDuration = 10,
            recordingPeriodList = arrayListOf(),
            durationSelected = ConfigureFragment.RECOMMENDED
        )

    }
}