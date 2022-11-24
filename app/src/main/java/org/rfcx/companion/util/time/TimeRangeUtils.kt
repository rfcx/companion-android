package org.rfcx.companion.util.time

import org.rfcx.companion.entity.time.Time
import org.rfcx.companion.entity.time.TimeRange

object TimeRangeUtils {

    fun toOppositeTimes(timeRanges: List<TimeRange>): List<TimeRange> {
        val temp = arrayListOf<TimeRange>()
        for (index in timeRanges.indices) {
            if (index == 0) {
                if (timeRanges.size == 1) {
                    if ((timeRanges[index].start.toIntValue() == 0)) {
                        temp.add(TimeRange(timeRanges[index].stop.plusOneMinute(), Time(23, 59)))
                        continue
                    }
                    if (timeRanges[index].stop.toIntValue() == 2359) {
                        temp.add(TimeRange(Time(0, 0), timeRanges[index].start.minusOneMinute()))
                        continue
                    }
                    temp.add(TimeRange(Time(0, 0), timeRanges[index].start.minusOneMinute()))
                    temp.add(TimeRange(timeRanges[index].stop.plusOneMinute(), Time(23, 59)))
                } else {
                    if (timeRanges[index].start.toIntValue() != 0) {
                        temp.add(TimeRange(Time(0, 0), timeRanges[index].start.minusOneMinute()))
                    }
                }
                continue
            }
            if (index == timeRanges.size - 1) {
                if (timeRanges[index].stop.toIntValue() != 2359) {
                    temp.add(TimeRange(timeRanges[index].stop.plusOneMinute(), Time(23, 59)))
                }
            }
            if (timeRanges.size != 1) {
                if (timeRanges[index - 1].stop.plusOneMinute() !=  timeRanges[index].start) {
                    temp.add(TimeRange(timeRanges[index - 1].stop, timeRanges[index].start.minusOneMinute()))
                }
            }
            continue
        }
        return ArrayList(temp.map { it.copy() }.sortedBy { it.start.toIntValue() })
    }

    fun simplifyTimes(timeRanges: List<TimeRange>): List<TimeRange> {
        if (timeRanges.size == 1) {
            return timeRanges
        }
        var needRedo = false
        var index = 0
        val copy = ArrayList(timeRanges.map { it.copy() }.sortedBy { it.start.toIntValue() })
        while (index < copy.size - 1) {
            if (isOutOfRange(copy[index], copy[index + 1])) {
                index++
                continue
            }
            if (isWithinRange(copy[index], copy[index + 1])) {
                copy.removeAt(index)
                needRedo = true
                index++
                continue
            }
            copy[index] = concatTimes(copy[index], copy[index + 1])
            copy.removeAt(index + 1)
            needRedo = true
            index++
            continue
        }
        return if (needRedo) {
            simplifyTimes(copy)
        } else {
            copy
        }
    }

    private fun isWithinRange(one: TimeRange, two: TimeRange): Boolean {
        return one.start.toIntValue() >= two.start.toIntValue() && one.start.toIntValue() <= two.stop.toIntValue() && one.stop.toIntValue() <= two.stop.toIntValue()
    }

    private fun isOutOfRange(one: TimeRange, two: TimeRange): Boolean {
        return (one.start.toIntValue() < two.start.toIntValue() && one.stop.toIntValue() < two.start.toIntValue()) || ((one.start.toIntValue() > two.stop.toIntValue()))
    }

    private fun concatTimes(one: TimeRange, two: TimeRange): TimeRange {
        if (two.start.toIntValue() >= one.start.toIntValue()) {
            if (two.start.toIntValue() <= one.stop.toIntValue()) {
                if (two.stop.toIntValue() <= one.stop.toIntValue()) {
                    // within range do nothing
                } else {
                    // expand exist stop to new stop
                    one.stop = two.stop
                }
            } else {
                return two
            }
        } else {
            if (two.stop.toIntValue() <= one.start.toIntValue()) {
                return two
            } else {
                if (two.stop.toIntValue() <= one.stop.toIntValue()) {
                    // expand exist start to new start
                    one.start = two.start
                } else {
                    // expand exist start to new start
                    // expand exist stop to new stop
                    one.start = two.start
                    one.stop = two.stop
                }
            }
        }
        return one
    }
}

fun ArrayList<TimeRange>.toGuardianFormat(): String {
    return TimeRangeUtils.toOppositeTimes(this).joinToString(",") { it.toStringFormat() }
}

fun String.toListTimeRange(): List<TimeRange> {
    val list = arrayListOf<TimeRange>()
    val time =
        "(?<starthh>\\d{1,2}):(?<startmm>\\d{1,2})-(?<stophh>\\d{1,2}):(?<stopmm>\\d{1,2})+,?".toRegex()
            .findAll(this)
    time.forEach {
        val (startHH, startMM, stopHH, stopMM) = it.destructured
        list.add(
            TimeRange(
                Time(startHH.toInt(), startMM.toInt()),
                Time(stopHH.toInt(), stopMM.toInt())
            )
        )
    }
    return list
}

fun String.toTimeRange(): TimeRange? {
    val time =
        "(?<starthh>\\d{1,2}):(?<startmm>\\d{1,2})-(?<stophh>\\d{1,2}):(?<stopmm>\\d{1,2})+,?".toRegex()
            .find(this)
    val (startHH, startMM, stopHH, stopMM) = time?.destructured ?: return null
    return TimeRange(Time(startHH.toInt(), startMM.toInt()), Time(stopHH.toInt(), stopMM.toInt()))
}
