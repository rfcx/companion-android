package org.rfcx.audiomoth.util

import org.rfcx.audiomoth.entity.EdgeDeployment

fun EdgeDeployment.getSampleRate(): AudioMothConfiguration.SampleRate {
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

fun EdgeDeployment.getGain(): AudioMothConfiguration.Gain {
    return when (this.configuration?.gain) {
        5 -> AudioMothConfiguration.Gain.HIGH_GAIN
        4 -> AudioMothConfiguration.Gain.MEDIUM_HIGH_GAIN
        3 -> AudioMothConfiguration.Gain.MEDIUM_GAIN
        2 -> AudioMothConfiguration.Gain.LOW_MEDIUM_GAIN
        else -> AudioMothConfiguration.Gain.LOW_GAIN
    }
}

fun EdgeDeployment.getSleepRecordCycle(): AudioMothConfiguration.SleepRecordCycle? {
    val sleepDuration = this.configuration?.sleepDuration
    val recordingDuration = this.configuration?.recordingDuration
    if (sleepDuration != null && recordingDuration != null) {
        return AudioMothConfiguration.SleepRecordCycle(sleepDuration, recordingDuration)
    }
    return null
}
