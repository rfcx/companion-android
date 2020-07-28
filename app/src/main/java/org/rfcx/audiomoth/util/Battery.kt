package org.rfcx.audiomoth.util

import kotlin.math.roundToInt

object Battery {
    const val BATTERY_PIN_GREEN = "BATTERY_PIN_GREEN"
    const val BATTERY_PIN_GREY = "BATTERY_PIN_GREY"

    private const val DAY = 24 * 60 * 60 * 1000

    fun getPredictionBattery(timestamp: Long): String {
        val currentMillis = System.currentTimeMillis()
        val daysLeft = if (timestamp > currentMillis) {
            val cal = ((timestamp - currentMillis) / (DAY).toDouble()).roundToInt()
            if (cal > 1) "$cal days" else "$cal day"
        } else {
            "<1 day"
        }
        return "$daysLeft remaining"
    }
}
