package org.rfcx.companion.entity.time

data class TimeRange(
    val start: Time,
    val stop: Time
) {
    fun toStringFormat(): String {
        return "${start.toStringFormat()}-${stop.toStringFormat()}"
    }
}

data class Time(
    val hour: Int = 0,
    val minute: Int = 0
) {
    fun toStringFormat(): String {
        val hour =
            if (this.hour.toString().length == 1) "0${this.hour}" else this.hour.toString()
        val minute =
            if (this.minute.toString().length == 1) "0${this.minute}" else this.minute.toString()
        return "$hour:$minute"
    }
}
