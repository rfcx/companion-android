package org.rfcx.audiomoth.util

import org.rfcx.audiomoth.entity.Deployment

fun Deployment.getSampleRate(): AudioMothConfiguration.SampleRate {
    return when (this.configuration?.sampleRate) {
        384 -> AudioMothConfiguration.SampleRate.SAMPLE_RATE_384KHZ
        256 -> AudioMothConfiguration.SampleRate.SAMPLE_RATE_256KHZ
        192 -> AudioMothConfiguration.SampleRate.SAMPLE_RATE_192KHZ
        96 -> AudioMothConfiguration.SampleRate.SAMPLE_RATE_96KHZ
        48 -> AudioMothConfiguration.SampleRate.SAMPLE_RATE_48KHZ
        32 -> AudioMothConfiguration.SampleRate.SAMPLE_RATE_32KHZ
        16 -> AudioMothConfiguration.SampleRate.SAMPLE_RATE_16KHZ
        else -> AudioMothConfiguration.SampleRate.SAMPLE_RATE_8KHZ
    }
}

fun Deployment.getGain(): AudioMothConfiguration.Gain {
    return when (this.configuration?.gain) {
        5 -> AudioMothConfiguration.Gain.HIGH_GAIN
        4 -> AudioMothConfiguration.Gain.MEDIUM_HIGH_GAIN
        3 -> AudioMothConfiguration.Gain.MEDIUM_GAIN
        2 -> AudioMothConfiguration.Gain.LOW_MEDIUM_GAIN
        else -> AudioMothConfiguration.Gain.LOW_GAIN
    }
}

fun Deployment.getSleepRecordCycle(): AudioMothConfiguration.SleepRecordCycle? {
    val sleepDuration = this.configuration?.sleepDuration
    val recordingDuration = this.configuration?.recordingDuration
    if (sleepDuration != null && recordingDuration != null) {
        return AudioMothConfiguration.SleepRecordCycle(sleepDuration, recordingDuration)
    }
    return null
}

fun Deployment.getStartStopPeriods(): Array<AudioMothConfiguration.StartStopPeriod>? {
    val recordingPeriodList = this.configuration?.recordingPeriodList
    if (recordingPeriodList?.size == 0) return arrayOf(
        AudioMothConfiguration.StartStopPeriod(
            0,
            1440
        )
    )
    val arrayTimeState = arrayListOf<Boolean>()
    recordingPeriodList.let {
        if (it != null) {
            EdgeConfigure.configureTimes.forEach { time ->
                arrayTimeState.add(it.contains(time))
            }
        }
    }
    return convertToStopStartPeriods(arrayTimeState.toTypedArray())
}

fun convertToStopStartPeriods(hours: Array<Boolean>): Array<AudioMothConfiguration.StartStopPeriod>? {
    val numberOfHours = 24
    val minutesInHour = 60
    val minutesInDay = 1440

    val numberOfSelectedHours = hours.fold(0) { sum, element -> sum + if (element) 1 else 0 }

    if (numberOfSelectedHours == 0) return null
    /* Handle special case where all hours are selected */
    if (numberOfSelectedHours == numberOfHours) return arrayOf(
        AudioMothConfiguration.StartStopPeriod(
            0,
            minutesInDay
        )
    )
    /* There is at least one start stop period */
    var firstHour = 0
    val startStopPeriods = arrayListOf<AudioMothConfiguration.StartStopPeriod>()
    /* Skip the start stop period that passes midnight for now */
    if (hours[numberOfHours - 1] && hours[firstHour]) while (hours[firstHour]) firstHour += 1
    /* Generate start stop periods */
    while (firstHour < numberOfHours) {
        /* Find the start of the period */
        if (!hours[firstHour]) {
            firstHour += 1
            continue
        }
        /* Find the end of the period */
        var duration = 0
        while (hours[(firstHour + duration) % numberOfHours]) duration += 1
        /* Add the period to the list */
        val startMinutes: Int = firstHour * minutesInHour
        var stopMinutes: Int = startMinutes + duration * minutesInHour
        if (stopMinutes > minutesInDay) stopMinutes -= minutesInDay
        startStopPeriods.add(AudioMothConfiguration.StartStopPeriod(startMinutes, stopMinutes))
        /* Look for the next one */
        firstHour += duration
    }
    /* Return array */
    return startStopPeriods.toTypedArray()
}
