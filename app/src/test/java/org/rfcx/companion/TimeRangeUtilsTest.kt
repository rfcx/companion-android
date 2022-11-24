package org.rfcx.companion

import junit.framework.Assert.assertEquals
import org.junit.Test
import org.rfcx.companion.entity.time.Time
import org.rfcx.companion.entity.time.TimeRange
import org.rfcx.companion.util.time.TimeRangeUtils

class TimeRangeUtilsTest {

    @Test
    fun canSimplifyTimesIfStartTimeWithinRange() {
        val exists = arrayListOf(
            TimeRange(Time(10, 0), Time(14, 0)),
            TimeRange(Time(13, 0), Time(15, 0))
        )

        val result = TimeRangeUtils.simplifyTimes(exists)

        assertEquals(1, result.size)
        assertEquals(exists[1].stop.hour, result[0].stop.hour)
        assertEquals(exists[1].stop.minute, result[0].stop.minute)
    }

    @Test
    fun canSimplifyTimesIfStartStopWithinRange1() {
        val exists = arrayListOf(
            TimeRange(Time(10, 0), Time(14, 0)),
            TimeRange(Time(11, 0), Time(13, 0))
        )

        val result = TimeRangeUtils.simplifyTimes(exists)

        assertEquals(1, result.size)
        assertEquals(exists[0].start.hour, result[0].start.hour)
        assertEquals(exists[0].start.minute, result[0].start.minute)
        assertEquals(exists[0].stop.hour, result[0].stop.hour)
        assertEquals(exists[0].stop.minute, result[0].stop.minute)
    }

    @Test
    fun canSimplifyTimesIfStartStopWithinRange2() {
        val exists = arrayListOf(
            TimeRange(Time(11, 0), Time(13, 0)),
            TimeRange(Time(10, 0), Time(14, 0))
        )

        val result = TimeRangeUtils.simplifyTimes(exists)

        assertEquals(1, result.size)
        assertEquals(exists[1].start.hour, result[0].start.hour)
        assertEquals(exists[1].start.minute, result[0].start.minute)
        assertEquals(exists[1].stop.hour, result[0].stop.hour)
        assertEquals(exists[1].stop.minute, result[0].stop.minute)
    }

    @Test
    fun canSimplifyTimesIfStartStopWithinRange3() {
        val exists = arrayListOf(
            TimeRange(Time(0, 0), Time(23, 0)),
            TimeRange(Time(11, 0), Time(13, 0)),
            TimeRange(Time(10, 0), Time(14, 0))
        )

        val result = TimeRangeUtils.simplifyTimes(exists)

        assertEquals(1, result.size)
        assertEquals(exists[0].start.hour, result[0].start.hour)
        assertEquals(exists[0].start.minute, result[0].start.minute)
        assertEquals(exists[0].stop.hour, result[0].stop.hour)
        assertEquals(exists[0].stop.minute, result[0].stop.minute)
    }

    @Test
    fun canSimplifyTimesIfStartStopWithinRange4() {
        val exists = arrayListOf(
            TimeRange(Time(11, 0), Time(13, 0)),
            TimeRange(Time(0, 0), Time(23, 0)),
            TimeRange(Time(10, 0), Time(14, 0))
        )

        val result = TimeRangeUtils.simplifyTimes(exists)

        assertEquals(1, result.size)
        assertEquals(exists[1].start.hour, result[0].start.hour)
        assertEquals(exists[1].start.minute, result[0].start.minute)
        assertEquals(exists[1].stop.hour, result[0].stop.hour)
        assertEquals(exists[1].stop.minute, result[0].stop.minute)
    }

    @Test
    fun canSimplifyTimesFrom3To2() {
        val exists = arrayListOf(
            TimeRange(Time(10, 0), Time(15, 0)),
            TimeRange(Time(13, 0), Time(16, 0)),
            TimeRange(Time(17, 0), Time(18, 0))
        )

        val result = TimeRangeUtils.simplifyTimes(exists)

        assertEquals(2, result.size)
        assertEquals(exists[0].start.hour, result[0].start.hour)
        assertEquals(exists[0].start.minute, result[0].start.minute)
        assertEquals(exists[1].stop.hour, result[0].stop.hour)
        assertEquals(exists[1].stop.minute, result[0].stop.minute)
        assertEquals(exists[2].start.hour, result[1].start.hour)
        assertEquals(exists[2].start.minute, result[1].start.minute)
        assertEquals(exists[2].stop.hour, result[1].stop.hour)
        assertEquals(exists[2].stop.minute, result[1].stop.minute)
    }

    @Test
    fun canSimplifyTimesFrom3To3() {
        val exists = arrayListOf(
            TimeRange(Time(10, 0), Time(15, 0)),
            TimeRange(Time(16, 0), Time(16, 30)),
            TimeRange(Time(17, 0), Time(18, 0))
        )

        val result = TimeRangeUtils.simplifyTimes(exists)

        assertEquals(3, result.size)
        assertEquals(exists[0].start.hour, result[0].start.hour)
        assertEquals(exists[0].start.minute, result[0].start.minute)
        assertEquals(exists[0].stop.hour, result[0].stop.hour)
        assertEquals(exists[0].stop.minute, result[0].stop.minute)
        assertEquals(exists[1].start.hour, result[1].start.hour)
        assertEquals(exists[1].start.minute, result[1].start.minute)
        assertEquals(exists[1].stop.hour, result[1].stop.hour)
        assertEquals(exists[1].stop.minute, result[1].stop.minute)
        assertEquals(exists[2].start.hour, result[2].start.hour)
        assertEquals(exists[2].start.minute, result[2].start.minute)
        assertEquals(exists[2].stop.hour, result[2].stop.hour)
        assertEquals(exists[2].stop.minute, result[2].stop.minute)
    }

    @Test
    fun canSimplifyTimesNewOverlapRange1() {
        val exists = arrayListOf(
            TimeRange(Time(10, 0), Time(15, 0)),
            TimeRange(Time(16, 0), Time(16, 30)),
            TimeRange(Time(17, 0), Time(18, 0)),
            TimeRange(Time(9, 0), Time(19, 0))
        )

        val result = TimeRangeUtils.simplifyTimes(exists)

        assertEquals(1, result.size)
        assertEquals(exists[3].start.hour, result[0].start.hour)
        assertEquals(exists[3].start.minute, result[0].start.minute)
        assertEquals(exists[3].stop.hour, result[0].stop.hour)
        assertEquals(exists[3].stop.minute, result[0].stop.minute)
    }

    @Test
    fun canSimplifyTimesNewOverlapRange2() {
        val exists = arrayListOf(
            TimeRange(Time(10, 0), Time(15, 0)),
            TimeRange(Time(16, 0), Time(16, 30)),
            TimeRange(Time(17, 0), Time(18, 0)),
            TimeRange(Time(9, 0), Time(19, 0)),
            TimeRange(Time(19, 0), Time(20, 0))
        )

        val result = TimeRangeUtils.simplifyTimes(exists)

        assertEquals(1, result.size)
        assertEquals(exists[3].start.hour, result[0].start.hour)
        assertEquals(exists[3].start.minute, result[0].start.minute)
        assertEquals(exists[4].stop.hour, result[0].stop.hour)
        assertEquals(exists[4].stop.minute, result[0].stop.minute)
    }

    @Test
    fun canSimplifyTimesNewOverlapRange3() {
        val exists = arrayListOf(
            TimeRange(Time(10, 0), Time(15, 0)),
            TimeRange(Time(16, 0), Time(16, 30)),
            TimeRange(Time(17, 0), Time(18, 0)),
            TimeRange(Time(9, 0), Time(19, 0)),
            TimeRange(Time(19, 1), Time(20, 0))
        )

        val result = TimeRangeUtils.simplifyTimes(exists)

        assertEquals(2, result.size)
        assertEquals(exists[3].start.hour, result[0].start.hour)
        assertEquals(exists[3].start.minute, result[0].start.minute)
        assertEquals(exists[3].stop.hour, result[0].stop.hour)
        assertEquals(exists[3].stop.minute, result[0].stop.minute)
        assertEquals(exists[4].start.hour, result[1].start.hour)
        assertEquals(exists[4].start.minute, result[1].start.minute)
        assertEquals(exists[4].stop.hour, result[1].stop.hour)
        assertEquals(exists[4].stop.minute, result[1].stop.minute)
    }

    @Test
    fun canConvertToOppositeRange1() {
        val exists = arrayListOf(
            TimeRange(Time(10, 0), Time(14, 0)),
            TimeRange(Time(15, 0), Time(16, 0))
        )

        val result = TimeRangeUtils.toOppositeTimes(exists)

        println(result)

        assertEquals(3, result.size)
        assertEquals(0, result[0].start.hour)
        assertEquals(0, result[0].start.minute)
        assertEquals(9, result[0].stop.hour)
        assertEquals(59, result[0].stop.minute)
        assertEquals(14, result[1].start.hour)
        assertEquals(1, result[1].start.minute)
        assertEquals(14, result[1].stop.hour)
        assertEquals(59, result[1].stop.minute)
        assertEquals(16, result[2].start.hour)
        assertEquals(1, result[2].start.minute)
        assertEquals(23, result[2].stop.hour)
        assertEquals(59, result[2].stop.minute)
    }

    @Test
    fun canConvertToOppositeRange2() {
        val exists = arrayListOf(
            TimeRange(Time(10, 0), Time(14, 0)),
            TimeRange(Time(15, 0), Time(16, 0)),
            TimeRange(Time(19, 1), Time(21, 59))
        )

        val result = TimeRangeUtils.toOppositeTimes(exists)

        assertEquals(4, result.size)
        assertEquals(0, result[0].start.hour)
        assertEquals(0, result[0].start.minute)
        assertEquals(9, result[0].stop.hour)
        assertEquals(59, result[0].stop.minute)
        assertEquals(14, result[1].start.hour)
        assertEquals(1, result[1].start.minute)
        assertEquals(14, result[1].stop.hour)
        assertEquals(59, result[1].stop.minute)
        assertEquals(16, result[2].start.hour)
        assertEquals(1, result[2].start.minute)
        assertEquals(19, result[2].stop.hour)
        assertEquals(0, result[2].stop.minute)
        assertEquals(22, result[3].start.hour)
        assertEquals(0, result[3].start.minute)
        assertEquals(23, result[3].stop.hour)
        assertEquals(59, result[3].stop.minute)
    }

    @Test
    fun canConvertToOppositeRange3() {
        val exists = arrayListOf(
            TimeRange(Time(0, 0), Time(9, 59)),
            TimeRange(Time(14, 1), Time(14, 59)),
            TimeRange(Time(16, 1), Time(19, 0)),
            TimeRange(Time(22, 0), Time(23, 59))
        )

        val result = TimeRangeUtils.toOppositeTimes(exists)

        assertEquals(3, result.size)
        assertEquals(10, result[0].start.hour)
        assertEquals(0, result[0].start.minute)
        assertEquals(14, result[0].stop.hour)
        assertEquals(0, result[0].stop.minute)
        assertEquals(15, result[1].start.hour)
        assertEquals(0, result[1].start.minute)
        assertEquals(16, result[1].stop.hour)
        assertEquals(0, result[1].stop.minute)
        assertEquals(19, result[2].start.hour)
        assertEquals(1, result[2].start.minute)
        assertEquals(21, result[2].stop.hour)
        assertEquals(59, result[2].stop.minute)
    }

    @Test
    fun canConvertToOppositeRange4() {
        val exists = arrayListOf(
            TimeRange(Time(0, 0), Time(9, 59))
        )

        val result = TimeRangeUtils.toOppositeTimes(exists)

        assertEquals(1, result.size)
        assertEquals(10, result[0].start.hour)
        assertEquals(0, result[0].start.minute)
        assertEquals(23, result[0].stop.hour)
        assertEquals(59, result[0].stop.minute)
    }

    @Test
    fun canConvertToOppositeRange6() {
        val exists = arrayListOf(
            TimeRange(Time(10, 0), Time(23, 59))
        )

        val result = TimeRangeUtils.toOppositeTimes(exists)

        assertEquals(1, result.size)
        assertEquals(0, result[0].start.hour)
        assertEquals(0, result[0].start.minute)
        assertEquals(9, result[0].stop.hour)
        assertEquals(59, result[0].stop.minute)
    }

    @Test
    fun canConvertToOppositeRange7() {
        val exists = arrayListOf(
            TimeRange(Time(23, 55), Time(23, 56)),
            TimeRange(Time(23, 57), Time(23, 59))
        )

        val result = TimeRangeUtils.toOppositeTimes(exists)

        assertEquals(1, result.size)
        assertEquals(0, result[0].start.hour)
        assertEquals(0, result[0].start.minute)
        assertEquals(23, result[0].stop.hour)
        assertEquals(54, result[0].stop.minute)
    }
    @Test
    fun canConvertToOppositeRange8() {
        val exists = arrayListOf(
            TimeRange(Time(0, 0), Time(23, 54))
        )

        val result = TimeRangeUtils.toOppositeTimes(exists)

        assertEquals(1, result.size)
        assertEquals(23, result[0].start.hour)
        assertEquals(55, result[0].start.minute)
        assertEquals(23, result[0].stop.hour)
        assertEquals(59, result[0].stop.minute)
    }

    @Test
    fun canConvertToOppositeRange9() {
        val exists = arrayListOf(
            TimeRange(Time(2, 0), Time(2, 54))
        )

        val result = TimeRangeUtils.toOppositeTimes(exists)

        assertEquals(2, result.size)
        assertEquals(0, result[0].start.hour)
        assertEquals(0, result[0].start.minute)
        assertEquals(1, result[0].stop.hour)
        assertEquals(59, result[0].stop.minute)
        assertEquals(2, result[1].start.hour)
        assertEquals(55, result[1].start.minute)
        assertEquals(23, result[1].stop.hour)
        assertEquals(59, result[1].stop.minute)

    }

}
