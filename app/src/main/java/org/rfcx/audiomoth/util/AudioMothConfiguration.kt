/****************************************************************************
 * AudioMothConfiguration.kt
 * openacousticdevices.info
 * June 2020
 *****************************************************************************/

package org.rfcx.audiomoth.util

class AudioMothConfiguration {

    /* Enumerations */

    enum class SampleRate {
        SAMPLE_RATE_8KHZ, SAMPLE_RATE_16KHZ, SAMPLE_RATE_32KHZ, SAMPLE_RATE_48KHZ, SAMPLE_RATE_96KHZ, SAMPLE_RATE_192KHZ, SAMPLE_RATE_256KHZ, SAMPLE_RATE_384KHZ
    }

    enum class Gain {
        LOW_GAIN, LOW_MEDIUM_GAIN, MEDIUM_GAIN, MEDIUM_HIGH_GAIN, HIGH_GAIN
    }

    /* Data classes */

    abstract class Filter

    data class LowPassFilter(var frequency: Int) : Filter()

    data class HighPassFilter(var frequency: Int) : Filter()

    data class BandPassFilter(var lowerFrequency: Int, var higherFrequency: Int) : Filter()

    data class AmplitudeThreshold(var threshold: Int)

    data class StartStopPeriod(var startMinutes: Int, var stopMinutes: Int)

    data class SleepRecordCycle(var sleepDuration: Int, var recordDuration: Int)

    data class RecordingDate(var day: Int, var month: Int, var year: Int)

    /* Default configuration */

    var sampleRate = SampleRate.SAMPLE_RATE_48KHZ

    var gain = Gain.MEDIUM_GAIN

    var sleepRecordCycle: SleepRecordCycle? = SleepRecordCycle(600, 300)

    var enableLED: Boolean = true

    var enableLowVoltageCutoff: Boolean = true

    var startStopPeriods: Array<StartStopPeriod>? = arrayOf(StartStopPeriod(0, 1440))

    var firstRecordingDate: RecordingDate? = null

    var lastRecordingDate: RecordingDate? = null

    var filter: Filter? = null

    var amplitudeThreshold: AmplitudeThreshold? = null

}