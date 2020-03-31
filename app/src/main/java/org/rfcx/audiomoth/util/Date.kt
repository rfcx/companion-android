package org.rfcx.audiomoth.util

import java.text.SimpleDateFormat
import java.util.*

private const val timeFormat = "HH:mm"

private val outputTimeSdf by lazy {
    val sdf = SimpleDateFormat(timeFormat, Locale.getDefault())
    sdf.timeZone = TimeZone.getDefault()
    sdf
}

fun Calendar.toTimeString(): String {
    return outputTimeSdf.format(this.time)
}

fun getCalendar() : Calendar {
    return Calendar.getInstance()
}