package org.rfcx.audiomoth.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.util.*
import kotlin.math.*


class ConnectDevice {
    private val sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
    private val minBufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    private var player = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .build()
    } else {
        AudioTrack(
            AudioManager.STREAM_MUSIC, sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize,
            AudioTrack.MODE_STATIC
        )
    }

    private val hamming = arrayOf(
        arrayOf(0, 0, 0, 0, 0, 0, 0),
        arrayOf(1, 1, 1, 0, 0, 0, 0),
        arrayOf(1, 0, 0, 1, 1, 0, 0),
        arrayOf(0, 1, 1, 1, 1, 0, 0),
        arrayOf(0, 1, 0, 1, 0, 1, 0),
        arrayOf(1, 0, 1, 1, 0, 1, 0),
        arrayOf(1, 1, 0, 0, 1, 1, 0),
        arrayOf(0, 0, 1, 0, 1, 1, 0),
        arrayOf(1, 1, 0, 1, 0, 0, 1),
        arrayOf(0, 0, 1, 1, 0, 0, 1),
        arrayOf(0, 1, 0, 0, 1, 0, 1),
        arrayOf(1, 0, 1, 0, 1, 0, 1),
        arrayOf(1, 0, 0, 0, 0, 1, 1),
        arrayOf(0, 1, 1, 0, 0, 1, 1),
        arrayOf(0, 0, 0, 1, 1, 1, 1),
        arrayOf(1, 1, 1, 1, 1, 1, 1)
    )

    private fun updateCRC(crc: Int, inCr: Int): Int {
        val crcPoly = 0x1021
        val xor = (crc shr 15) and 65535
        var out = crc shl 1 and 65535

        if (inCr > 0) {
            out += 1
        }

        if (xor > 0) {
            out = out xor crcPoly
        }
        return out
    }

    private fun createCRC16(bytes: ArrayList<Int>): ArrayList<Int> {
        var crc = 0
        bytes.forEach {
            for (x in 7 downTo 0) {
                crc = updateCRC(crc, (it and (1 shl x)))
            }
        }

        for (i in 0 until 16) {
            crc = updateCRC(crc, 0)
        }

        val low = crc and 255
        val high = crc shr 8 and 255
        return arrayListOf(low, high)
    }

    private fun encode(bytes: ArrayList<Int>): ArrayList<Int> {
        val bitSequence = arrayListOf<Int>()
        bytes.forEach {
            val low = (it and 0x0F)
            val high = it and 0xFF shr 4

            for (x in 0 until 7) {
                bitSequence.add(hamming[low][x])
                bitSequence.add(hamming[high][x])
            }
        }
        return bitSequence
    }

    private fun createWaveform(
        state: WaveformState, frequency: Double, phase: Int, rampUp: Double,
        sustain: Double, rampDown: Double
    ): Pair<ArrayList<Float>, WaveformState> {
        val waveform = arrayListOf<Float>()
        val samplesInRampUp = rampUp * sampleRate
        val samplesInSustain = sustain * sampleRate
        val samplesInRampDown = rampDown * sampleRate
        val theta = 2f * Math.PI.toFloat() * frequency / sampleRate
        val waveSize = round(samplesInRampUp + samplesInSustain + samplesInRampDown).toInt()

        for (k in 0 until waveSize) {

            if (k < ceil(samplesInRampUp)) {
                state.amplitudePhase =
                    min(
                        Math.PI.toFloat() / 2.0f,
                        state.amplitudePhase + Math.PI.toFloat() / 2.0f / samplesInRampUp.toFloat()
                    )
            }

            if (k >= ceil(samplesInRampUp + samplesInSustain)) {
                state.amplitudePhase =
                    max(
                        0.0f,
                        state.amplitudePhase - Math.PI.toFloat() / 2.0f / samplesInRampDown.toFloat()
                    )
            }

            val volume = sin(state.amplitudePhase).pow(10f)
            waveform.add((volume * phase * state.x))

            val x = (state.x * cos(theta).toFloat()) - (state.y * sin(theta).toFloat())
            val y = (state.x * sin(theta).toFloat()) + (state.y * cos(theta).toFloat())
            state.x = x
            state.y = y
        }
        return Pair(waveform, state)
    }

    private fun getWaveform(): ArrayList<Float> {
        val sumWaveform = arrayListOf<Float>()
        val waveform1 = arrayListOf<Float>()
        val waveform2 = arrayListOf<Float>()

        val calendar = Calendar.getInstance()
        val unixTime = (calendar.timeInMillis / 1000).toInt()
        val timezoneOffset = calendar.timezoneOffset()
        val timezoneHours = (timezoneOffset / 60)
        val timezoneMinutes = (timezoneOffset % 60)
        val bytes = arrayListOf<Int>().apply {
            addAll(unixTime.toBytes(4))
            addAll(timezoneHours.toBytes(1))
            addAll(timezoneMinutes.toBytes(1))
        }
        // encrypt
        bytes.addAll(createCRC16(bytes))
        bytes.addAll(0.toBytes(4))
        val bitSequence = encode(bytes = bytes)

        Log.i(TAG, "Unix time: $unixTime")
        Log.i(TAG, "Timezone offset: $timezoneOffset")
        Log.i(TAG, "Timezone hours: $timezoneHours")
        Log.i(TAG, "Timezone minutes: $timezoneMinutes")
        Log.i(TAG, "Bytes: $bytes")
        Log.i(TAG, "Number of Bits: ${bitSequence.size}")

        // Sound wave creation
        val frequency = 18000.0
        var waveState1 = WaveformState()

        // Sound wave creation - high tone(wave1)
        var wave1: Pair<ArrayList<Float>, WaveformState>
        for (h in 0 until 5) {
            wave1 = createWaveform(waveState1, frequency, 1, 0.0005, 0.0065, 0.0005)
            waveState1 = wave1.second
            waveform1.addAll(wave1.first)

            wave1 = createWaveform(waveState1, frequency, -1, 0.0005, 0.0065, 0.0005)
            waveState1 = wave1.second
            waveform1.addAll(wave1.first)
        }

        var phase = 1
        bitSequence.forEach {
            if (it == 1) {
                wave1 = createWaveform(waveState1, frequency, phase, 0.0005, 0.009, 0.0005)
                waveState1 = wave1.second
                waveform1.addAll(wave1.first)
            } else {
                wave1 = createWaveform(waveState1, frequency, phase, 0.0005, 0.004, 0.0005)
                waveState1 = wave1.second
                waveform1.addAll(wave1.first)
            }
            phase *= -1
        }
        wave1 = createWaveform(waveState1, frequency, phase, 0.0005, 0.004, 0.0005)
        waveState1 = wave1.second
        waveform1.addAll(wave1.first)

        // Sound wave creation - wave2
        val multiplier = 1.25
        var wave2: Pair<ArrayList<Float>, WaveformState>
        var waveState2 = WaveformState()

        wave2 = createWaveform(waveState2, multiplier * 261.63, 1, 0.030, 0.120, 0.030)
        waveState2 = wave2.second
        waveform2.addAll(wave2.first)

        wave2 = createWaveform(waveState2, multiplier * 293.66, 1, 0.030, 0.120, 0.030)
        waveState2 = wave2.second
        waveform2.addAll(wave2.first)

        wave2 = createWaveform(waveState2, multiplier * 329.63, 1, 0.030, 0.120, 0.030)
        waveState2 = wave2.second
        waveform2.addAll(wave2.first)

        val duration = ((waveform1.size - waveform2.size) - 0.120 * sampleRate) / sampleRate
        wave2 = createWaveform(
            waveState2,
            multiplier * 261.63,
            1,
            0.030,
            duration,
            0.090
        )
        waveform2.addAll(wave2.first)

        // Sound wave creation - sum the waveforms
        for (index in 0 until waveform1.size) {
            sumWaveform.add(waveform1[index] / 4.0f + waveform2[index] / 2.0f)
        }

        return sumWaveform
    }

    fun playSound(context: Context) {
        val waveform = getWaveform()

        // Start play
        player.play()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.i(TAG, "Wave Form: $waveform")

            val buffer = ShortArray(waveform.size)
            waveform.forEachIndexed { index, fl ->
                buffer[index] = (fl * Short.MAX_VALUE).toShort()
            }

            player.write(buffer, 0, waveform.size)
        } else {
            Toast.makeText(context, "No working for Android APIs lower 21.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun Int.toBytes(byteCount: Int): ArrayList<Int> {
        val buffer = arrayListOf<Int>()
        for (index in 0 until byteCount) {
            buffer.add((this shr (index * 8)) and 255)
        }
        return buffer
    }

    private fun Calendar.timezoneOffset(): Int {
        return (this.get(Calendar.ZONE_OFFSET) + this.get(Calendar.DST_OFFSET)) / (60 * 1000)
    }

    companion object {
        private const val TAG = "ConnectDevice"
    }
}

data class WaveformState(var amplitudePhase: Float = 0.0f, var x: Float = 1.0f, var y: Float = 0.0f)
