package org.rfcx.companion.util

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import org.rfcx.companion.R
import java.text.SimpleDateFormat
import java.util.*

private const val timeFormat = "HH:mm"
private const val standardDateFormat = "MMMM d, yyyy HH:mm"
private const val dateFormat = "dd/MM/yyyy HH:mm"
private const val iso8601Format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

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

private val iso8601DateSdf by lazy {
    val sdf = SimpleDateFormat(iso8601Format, Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
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

fun Date.toTimeString(): String {
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

fun Date.toISO8601Format(): String {
    return iso8601DateSdf.format(this)
}
