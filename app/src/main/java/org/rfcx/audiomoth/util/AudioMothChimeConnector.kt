package org.rfcx.audiomoth.util

import java.util.Calendar
import kotlin.random.Random
import kotlin.math.*

class AudioMothChimeConnector : AudioMothConnector {

    /* Useful constants */

    private val BITS_PER_BYTE: Int = 8
    private val BITS_IN_INT16: Int = 16
    private val BITS_IN_INT32: Int = 32

    private val UINT16_MAX: Int = 0xFFFF

    private val HOURS_IN_DAY: Int = 24
    private val MINUTES_IN_HOUR: Int = 60
    private val SECONDS_IN_MINUTE: Int = 60

    private val MILLISECONDS_IN_SECOND: Int = 1000

    private val SECONDS_IN_DAY: Int = SECONDS_IN_MINUTE * MINUTES_IN_HOUR * HOURS_IN_DAY

    private val GET_BATTERY_MESSAGE_LENGTH: Int = 6
    private val COMPRESSED_CONFIGURATION_LENGTH: Int = 8
    private val MAXIMUM_SET_CONFIGURATION_MESSAGE_LENGTH: Int = 54

    private val UNIQUE_ID_LENGTH: Int = 8
    private val MAX_START_STOP_PERIODS: Int = 5

    private val MINIMUM_SLEEP_DURATION: Int = 5
    private val MINIMUM_RECORD_DURATION: Int = 1

    private val MAXIMUM_DURATION: Int = 43200
    private val MAXIMUM_FILTER_THRESHOLD: Int = 32768
    private val MAXIMUM_START_STOP_MINUTES: Int = 1440
    private val MAXIMUM_FILTER_FREQUENCY: Int = 192000

    private val FILTER_FREQUENCY_FACTOR: Int = 100

    /* AudioMothChime object */

    private val audioMothChime = AudioMothChime()

    /* Data class to keep track of packet contents */

    private data class State(var bytes: Array<Int>, var index: Int)

    /* Private functions to set data */

    private fun setBit(state: State, value: Boolean) {

        val byte = state.index / BITS_PER_BYTE
        val bit = state.index % BITS_PER_BYTE

        if (value) {

            state.bytes[byte] = state.bytes[byte] or (1 shl bit)

        }

        state.index += 1

    }

    private fun setBits(state: State, value: Int, length: Int) {

        for (i in 0 until length) {

            val mask = (1 shl i)

            setBit(state, (value and mask) == mask)

        }

    }

    /* Public interface functions */

    override fun getBatteryState() {

        val data = Array<Int>(GET_BATTERY_MESSAGE_LENGTH) { Random.nextInt(256) }

        audioMothChime.chime(
            data,
            arrayOf(
                "c4:4",
                "e4:4"
            )
        )

    }

    override fun getPacketLength(configuration: AudioMothConfiguration): Int {

        var packetLength: Int = COMPRESSED_CONFIGURATION_LENGTH + UNIQUE_ID_LENGTH

        packetLength += if (configuration.sleepRecordCycle == null) 0 else 2 * BITS_IN_INT16 / BITS_PER_BYTE

        if (configuration.startStopPeriods != null) {

            val startStopPeriods = configuration.startStopPeriods as Array<AudioMothConfiguration.StartStopPeriod>

            val numberOfStartStopPeriod = min(MAX_START_STOP_PERIODS, startStopPeriods.size)

            packetLength += 2 * numberOfStartStopPeriod * BITS_IN_INT16 / BITS_PER_BYTE

        }

        packetLength += if (configuration.firstRecordingDate == null) 0 else BITS_IN_INT32 / BITS_PER_BYTE

        packetLength += if (configuration.lastRecordingDate == null) 0 else BITS_IN_INT32 / BITS_PER_BYTE

        packetLength += if (configuration.filter == null) 0 else 2 * BITS_IN_INT16 / BITS_PER_BYTE

        packetLength += if (configuration.amplitudeThreshold == null) 0 else BITS_IN_INT16 / BITS_PER_BYTE

        return packetLength

    }

    override fun setConfiguration(calendar: Calendar, configuration: AudioMothConfiguration, id: Array<Int>?) {

        /* Calculate timestamp and offset */

        val timestamp: Int = (calendar.timeInMillis / MILLISECONDS_IN_SECOND).toInt()

        val timezoneMinutes: Int = (calendar.timeZone.rawOffset + calendar.timeZone.dstSavings) / SECONDS_IN_MINUTE / MILLISECONDS_IN_SECOND

        /* Set up array */

        var data = Array<Int>(MAXIMUM_SET_CONFIGURATION_MESSAGE_LENGTH) { 0 }

        var state = State(data, 0)

        /* Time and timezone */

        setBits(state, timestamp, BITS_IN_INT32)

        setBits(state, timezoneMinutes, BITS_IN_INT16)

        /* Gain */

        setBits(state, configuration.gain.ordinal, 3)

        /* Sample rate */

        setBits(state, configuration.sampleRate.ordinal, 3)

        /* Low voltage cut-off enable */

        setBit(state, configuration.enableLowVoltageCutoff)

        /* Sleep record disable */

        setBit(state, configuration.sleepRecordCycle == null)

        /* Enable LED */

        setBit(state, configuration.enableLED)

        /* Active start stop periods */

        var activeStartStopPeriods: Int = 0

        if (configuration.startStopPeriods != null) {

            activeStartStopPeriods = min(MAX_START_STOP_PERIODS, (configuration.startStopPeriods as Array<AudioMothConfiguration.StartStopPeriod>).size)

        }

        setBits(state, activeStartStopPeriods, 3)

        /* Earliest and latest recording times */

        setBit(state, configuration.firstRecordingDate != null)

        setBit(state, configuration.lastRecordingDate != null)

        /* Filter */

        setBit(state, configuration.filter != null)

        /* Amplitude threshold */

        setBit(state, configuration.amplitudeThreshold != null)

        /* Sleep and record duration */

        if (configuration.sleepRecordCycle != null) {

            val sleepDuration = max(MINIMUM_SLEEP_DURATION, min(MAXIMUM_DURATION, (configuration.sleepRecordCycle as AudioMothConfiguration.SleepRecordCycle).sleepDuration))

            val recordDuration = max(MINIMUM_RECORD_DURATION, min(MAXIMUM_DURATION, (configuration.sleepRecordCycle as AudioMothConfiguration.SleepRecordCycle).recordDuration))

            setBits(state, sleepDuration, BITS_IN_INT16)

            setBits(state, recordDuration, BITS_IN_INT16)

        }

        /* Start stop periods */

        if (activeStartStopPeriods > 0) {

            val startStopPeriods = configuration.startStopPeriods as Array<AudioMothConfiguration.StartStopPeriod>

            for (i in 0 until activeStartStopPeriods) {

                setBits(state, max(0, min(MAXIMUM_START_STOP_MINUTES, startStopPeriods[i].startMinutes)), BITS_IN_INT16)

                setBits(state, max(0, min(MAXIMUM_START_STOP_MINUTES, startStopPeriods[i].stopMinutes)), BITS_IN_INT16)

            }

        }

        /* Earliest recording time */

        if (configuration.firstRecordingDate != null) {

            val firstRecordingDate = configuration.firstRecordingDate as AudioMothConfiguration.RecordingDate

            val earliestRecordingCalendar: Calendar = calendar.clone() as Calendar

            earliestRecordingCalendar.set(
                firstRecordingDate.year,
                firstRecordingDate.month - 1,
                firstRecordingDate.day,
                0,
                0,
                0
            )

            val earliestRecordingTime = (earliestRecordingCalendar.timeInMillis / MILLISECONDS_IN_SECOND).toInt()

            setBits(state, earliestRecordingTime, BITS_IN_INT32)

        }

        /* Latest recording time */

        if (configuration.lastRecordingDate != null) {

            val lastRecordingDate = configuration.firstRecordingDate as AudioMothConfiguration.RecordingDate

            val latestRecordingCalendar = calendar.clone() as Calendar

            latestRecordingCalendar.set(
                lastRecordingDate.year,
                lastRecordingDate.month - 1,
                lastRecordingDate.day,
                0,
                0,
                0
            )

            val latestRecordingTime = (latestRecordingCalendar.timeInMillis / MILLISECONDS_IN_SECOND).toInt() + SECONDS_IN_DAY

            setBits(state, latestRecordingTime, BITS_IN_INT32)

        }

        /* Filter */

        if (configuration.filter is AudioMothConfiguration.LowPassFilter) {

            val lowerFilterFrequency = UINT16_MAX
            val higherFilterFrequency = max(0, min(MAXIMUM_FILTER_FREQUENCY, (configuration.filter as AudioMothConfiguration.LowPassFilter).frequency)) / FILTER_FREQUENCY_FACTOR

            setBits(state, lowerFilterFrequency, BITS_IN_INT16)
            setBits(state, higherFilterFrequency, BITS_IN_INT16)

        } else if (configuration.filter is AudioMothConfiguration.HighPassFilter) {

            val lowerFilterFrequency = max(0, min(MAXIMUM_FILTER_FREQUENCY, (configuration.filter as AudioMothConfiguration.HighPassFilter).frequency)) / FILTER_FREQUENCY_FACTOR
            val higherFilterFrequency = UINT16_MAX

            setBits(state, lowerFilterFrequency, BITS_IN_INT16)
            setBits(state, higherFilterFrequency, BITS_IN_INT16)

        } else if (configuration.filter is AudioMothConfiguration.BandPassFilter) {

            val lowerFilterFrequency = max(0, min(MAXIMUM_FILTER_FREQUENCY, (configuration.filter as AudioMothConfiguration.BandPassFilter).lowerFrequency)) / FILTER_FREQUENCY_FACTOR
            val higherFilterFrequency = max(0, min(MAXIMUM_FILTER_FREQUENCY, (configuration.filter as AudioMothConfiguration.BandPassFilter).higherFrequency)) / FILTER_FREQUENCY_FACTOR

            setBits(state, lowerFilterFrequency, BITS_IN_INT16)
            setBits(state, higherFilterFrequency, BITS_IN_INT16)

        }

        /* Amplitude threshold */

        if (configuration.amplitudeThreshold != null) {

            val amplitudeThreshold = max(0, min(MAXIMUM_FILTER_THRESHOLD, (configuration.amplitudeThreshold as AudioMothConfiguration.AmplitudeThreshold).threshold))

            setBits(state, amplitudeThreshold, BITS_IN_INT16)

        }

        /* Unique ID */

        val length = state.index / BITS_PER_BYTE + UNIQUE_ID_LENGTH

        if (id != null) {

            for (i in 0 until min(UNIQUE_ID_LENGTH, id.size)) {

                data[length - 1 - i] = id[i] and 0xFF

            }

        }

        /* Play the data */

        data = data.slice(0 until length).toTypedArray()

        audioMothChime.chime(
            data,
            arrayOf(
                "c4:4",
                "d4:4",
                "e4:4",
                "c4:4",
                "c4:4",
                "d4:4",
                "e4:4"
            )
        )

    }

}
