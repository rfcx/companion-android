package org.rfcx.companion.util

import java.util.concurrent.TimeUnit

object TimeAgo {
    val times: List<Long> = listOf(
        TimeUnit.DAYS.toMillis(365),
        TimeUnit.DAYS.toMillis(30),
        TimeUnit.DAYS.toMillis(1),
        TimeUnit.HOURS.toMillis(1),
        TimeUnit.MINUTES.toMillis(1),
        TimeUnit.SECONDS.toMillis(1)
    )
    private val timesString: List<String> =
        listOf("year", "month", "day", "hour", "minute", "second")

    fun toDuration(duration: Long?): String? {
        val res = StringBuffer()
        if (duration != null) {
            for (i in times.indices) {
                val current = times[i]
                val temp = duration / current
                if (temp > 0) {
                    res.append(temp).append(" ").append(timesString[i])
                        .append(if (temp != 1L) "s" else "").append(" ago")
                    break
                }
            }
            return if ("" == res.toString()) "Just now" else res.toString()
        }
        return "-"
    }
}
