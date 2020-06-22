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
    val recordingPeriods = recordingPeriodList?.map { it.toDate().toIsoString() }
    var array: Array<AudioMothConfiguration.StartStopPeriod> = arrayOf()

    val timeList = arrayListOf(
        "00:00",
        "01:00",
        "02:00",
        "03:00",
        "04:00",
        "05:00",
        "06:00",
        "07:00",
        "08:00",
        "09:00",
        "10:00",
        "11:00",
        "12:00",
        "13:00",
        "14:00",
        "15:00",
        "16:00",
        "17:00",
        "18:00",
        "19:00",
        "20:00",
        "21:00",
        "22:00",
        "23:00"
    )
    recordingPeriods?.let {
        val arrayStartStopPeriod = arrayListOf<AudioMothConfiguration.StartStopPeriod>()
        var timeCount = 0
        for (time in it) {
            val index = timeList.indexOf(time)
            if (time == it[it.size - 1]) {
                arrayStartStopPeriod.add(
                    AudioMothConfiguration.StartStopPeriod(
                        (index - timeCount) * 60,
                        (index * 60) + 60
                    )
                )
                timeCount = 0
            } else if (index == 23) {
                arrayStartStopPeriod.add(
                    AudioMothConfiguration.StartStopPeriod(
                        (index - timeCount) * 60,
                        (index * 60) + 60
                    )
                )
                timeCount = 0
            } else if (!it.contains(timeList[index + 1])) {
                arrayStartStopPeriod.add(
                    AudioMothConfiguration.StartStopPeriod(
                        (index - timeCount) * 60,
                        (index * 60) + 60
                    )
                )
                timeCount = 0
            } else {
                timeCount += 1
            }
        }
        array = arrayStartStopPeriod.toTypedArray()
    }
    return array
}