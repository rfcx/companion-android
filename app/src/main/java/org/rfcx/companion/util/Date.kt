package org.rfcx.companion.util

import java.text.SimpleDateFormat
import java.util.*

private const val timeFormat = "HH:mm"
private const val standardDateFormat = "MMMM d, yyyy HH:mm"
private const val dateFormat = "dd/MM/yyyy HH:mm"

private val outputTimeSdf by lazy {
    val sdf = SimpleDateFormat(timeFormat, Locale.getDefault())
    sdf.timeZone = TimeZone.getDefault()
    sdf
}

private val outputStandardDateSdf by lazy {
    val sdf = SimpleDateFormat(standardDateFormat, Locale.getDefault())
    sdf.timeZone = TimeZone.getDefault()
    sdf
}
private val outputDateSdf by lazy {
    val sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
    sdf.timeZone = TimeZone.getDefault()
    sdf
}

private val isoSdf by lazy {
    val sdf = SimpleDateFormat("HH:mm", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    sdf
}

fun Date.toDateTimeString(): String {
    return outputStandardDateSdf.format(this)
}

fun Date.toDateString(): String {
    return outputDateSdf.format(this)
}

fun Calendar.toTimeString(): String {
    return outputTimeSdf.format(this.time)
}

fun getCalendar(): Calendar {
    return Calendar.getInstance()
}

fun Date.toIsoString(): String {
    return isoSdf.format(this)
}

fun String.toDate(): Date {
    return outputTimeSdf.parse(this)
}
