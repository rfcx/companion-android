package org.rfcx.audiomoth.util

import android.content.Context
import kotlin.math.roundToInt
import org.rfcx.audiomoth.R

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

    fun getEstimatedBatteryDuration(context: Context, timestamp: Long): String {
        val currentMillis = System.currentTimeMillis()
        return if (timestamp > currentMillis) {
            val numberOfDate = ((timestamp - currentMillis) / DAY.toDouble()).roundToInt()
            when {
                numberOfDate > 1 -> context.getString(R.string.days_remaining, numberOfDate)
                numberOfDate == 1 -> context.getString(
                    R.string.day_remaining, numberOfDate.toString()
                )
                else -> context.getString(R.string.day_remaining, "<1")
            }
        } else {
            context.getString(R.string.battery_depleted)
        }
    }

    fun getEstimatedBatteryDays(timestamp: Long): Int {
        val currentMillis = System.currentTimeMillis()
        return if (timestamp > currentMillis) {
            ((timestamp - currentMillis) / DAY.toDouble()).roundToInt()
        } else {
            0
        }
    }
}
