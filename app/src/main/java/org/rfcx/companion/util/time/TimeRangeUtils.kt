package org.rfcx.companion.util.time

import org.rfcx.companion.entity.time.Time
import org.rfcx.companion.entity.time.TimeRange

object TimeRangeUtils {


}

fun String.toListTimeRange(): List<TimeRange> {
    val list = arrayListOf<TimeRange>()
    val time = "(?<starthh>\\d{1,2})[:](?<startmm>\\d{1,2})-(?<stophh>\\d{1,2})[:](?<stopmm>\\d{1,2})+,?".toRegex().findAll(this)
    time.forEach {
        val (startHH, startMM, stopHH, stopMM) = it.destructured
        list.add(TimeRange(Time(startHH.toInt(), startMM.toInt()), Time(stopHH.toInt(), stopMM.toInt())))
    }
    return list
}

fun String.toTimeRange(): TimeRange? {
    val time = "(?<starthh>\\d{1,2})[:](?<startmm>\\d{1,2})-(?<stophh>\\d{1,2})[:](?<stopmm>\\d{1,2})+,?".toRegex().find(this)
    val (startHH, startMM, stopHH, stopMM) = time?.destructured ?: return null
    return TimeRange(Time(startHH.toInt(), startMM.toInt()), Time(stopHH.toInt(), stopMM.toInt()))
}
