package org.rfcx.audiomoth.util

object Battery {
    const val BATTERY_PIN_GREEN = "BATTERY_PIN_GREEN"
    const val BATTERY_PIN_ORANGE = "BATTERY_PIN_ORANGE"
    const val BATTERY_PIN_RED = "BATTERY_PIN_RED"

    fun getBatteryPinImage(timestamp: Long): String {
        val currentMillis = System.currentTimeMillis()
        val threeDays = 3 * 24 * 60 * 60 * 1000
        val oneDay = 24 * 60 * 60 * 1000

        return if (timestamp > (currentMillis + threeDays)) {
            BATTERY_PIN_GREEN
        } else if (timestamp > (currentMillis + oneDay) && timestamp < (currentMillis + threeDays)) {
            BATTERY_PIN_ORANGE
        } else {
            BATTERY_PIN_RED
        }
    }
}