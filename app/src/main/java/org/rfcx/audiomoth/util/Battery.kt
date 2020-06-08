package org.rfcx.audiomoth.util

object Battery {
    const val BATTERY_PIN_GREEN = "BATTERY_PIN_GREEN"
    const val BATTERY_PIN_ORANGE = "BATTERY_PIN_ORANGE"
    const val BATTERY_PIN_RED = "BATTERY_PIN_RED"
    const val BATTERY_PIN_GREY = "BATTERY_PIN_GREY"

    private const val DAY = 24 * 60 * 60 * 1000

    fun getBatteryPinImage(timestamp: Long): String {
        val currentMillis = System.currentTimeMillis()
        val threeDays = 3 * DAY
        return if (timestamp > (currentMillis + threeDays)) {
            BATTERY_PIN_GREEN
        } else if (timestamp > (currentMillis + DAY) && timestamp < (currentMillis + threeDays)) {
            BATTERY_PIN_ORANGE
        } else {
            BATTERY_PIN_RED
        }
    }

    fun getPredictionBattery(timestamp: Long): String {
        val currentMillis = System.currentTimeMillis()
        val threeDays = 3 * DAY
        val daysLeft = if (timestamp > (currentMillis + threeDays)) {
            "3+ days"
        } else if (timestamp > (currentMillis + DAY) && timestamp < (currentMillis + threeDays)) {
            "1-2 days"
        } else {
            "<1 day"
        }
        return "$daysLeft (remaining)"
    }
}