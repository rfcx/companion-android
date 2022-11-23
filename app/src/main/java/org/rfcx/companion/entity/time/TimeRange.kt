package org.rfcx.companion.entity.time

data class TimeRange(
    var start: Time,
    var stop: Time
) {
    fun toStringFormat(): String {
        return "${start.toStringFormat()}-${stop.toStringFormat()}"
    }
}

data class Time(
    var hour: Int = 0,
    var minute: Int = 0
) {
    fun toStringFormat(): String {
        val hour =
            if (this.hour.toString().length == 1) "0${this.hour}" else this.hour.toString()
        val minute =
            if (this.minute.toString().length == 1) "0${this.minute}" else this.minute.toString()
        return "$hour:$minute"
    }

    fun toIntValue(): Int {
        val hour =
            if (this.hour.toString().length == 1) "0${this.hour}" else this.hour.toString()
        val minute =
            if (this.minute.toString().length == 1) "0${this.minute}" else this.minute.toString()
        return "$hour$minute".toInt()
    }

    fun plusOneMinute(): Time {
        if (this.minute == 59) {
            this.minute = 0
            this.hour = this.hour + 1
        } else {
            this.minute = this.minute + 1
        }
        return this
    }

    fun minusOneMinute(): Time {
        if (this.minute == 0) {
            this.minute = 59
            this.hour = this.hour - 1
        } else {
            this.minute = this.minute - 1
        }
        return this
    }
}
