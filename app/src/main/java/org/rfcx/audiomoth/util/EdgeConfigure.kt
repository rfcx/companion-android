package org.rfcx.audiomoth.util

import org.rfcx.audiomoth.view.deployment.configure.ConfigureFragment

object EdgeConfigure {
    const val GAIN_DEFAULT = 2
    const val SAMPLE_RATE_DEFAULT = 48
    const val RECORDING_DURATION_DEFAULT = 300
    const val SLEEP_DURATION_DEFAULT = 600
    const val DURATION_SELECTED_DEFAULT = ConfigureFragment.RECOMMENDED

    val configureSampleRate = arrayOf("8", "16", "32", "48", "96", "192", "256", "384")
    val configureTimes = arrayListOf(
        "00:00",
        "01:00",
        "02:00",
        "03:00",
        "04:00",
        "05:00",
        "06:00",
        "07:00",
        "08:00",
        "09:00",
        "10:00",
        "11:00",
        "12:00",
        "13:00",
        "14:00",
        "15:00",
        "16:00",
        "17:00",
        "18:00",
        "19:00",
        "20:00",
        "21:00",
        "22:00",
        "23:00"
    )
}
