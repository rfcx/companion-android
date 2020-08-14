package org.rfcx.audiomoth.util.spectrogram

import org.jtransforms.fft.FloatFFT_1D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.min
import kotlin.math.sqrt


object AudioSpectrogramUtils {

    private enum class ScrollSpeed(val value: Int) { FAST(1024), NORMAL(2048), SLOW(4096)}

    private var fftResolution = 1024

    private var bufferStack: ArrayList<ShortArray>? = null
    private var fftBuffer: ShortArray? = null
    private var isSetup = false

    fun getFFTResolution(): Int = fftResolution

    fun resetToDefaultValue() {
        fftResolution = 1024
    }

    fun resetSetupState() {
        isSetup = false
    }

    fun setSpeed(speed: String) {
        when (speed.toUpperCase(Locale.getDefault())) {
            ScrollSpeed.FAST.name -> fftResolution = ScrollSpeed.FAST.value
            ScrollSpeed.NORMAL.name -> fftResolution = ScrollSpeed.NORMAL.value
            ScrollSpeed.SLOW.name -> fftResolution = ScrollSpeed.SLOW.value
        }
    }

    fun setupSpectrogram(bufferLength: Int) {
        if (!isSetup) {
            val res = fftResolution
            fftBuffer = ShortArray(res)
            bufferStack = arrayListOf()
            val size = (bufferLength / (res / 2)) / 4
            for (i in 0 until size + 1) {
                bufferStack!!.add(ShortArray((res / 2)))
            }
            isSetup = true
        }
    }

    fun getTrunks(recordBuffer: ShortArray, spectrogramListener: SpectrogramListener) {
        val n = fftResolution
        if (bufferStack != null) {
            // Trunks are consecutive n/2 length samples
            for (i in 0 until bufferStack!!.size - 1) {
                System.arraycopy(
                    recordBuffer,
                    n / 2 * i,
                    bufferStack!![i + 1],
                    0,
                    n / 2
                )
            }

            // Build n length buffers for processing
            // Are build from consecutive trunks
            for (i in 0 until bufferStack!!.size - 1) {
                System.arraycopy(bufferStack!![i], 0, fftBuffer!!, 0, n / 2)
                System.arraycopy(bufferStack!![i + 1], 0, fftBuffer!!, n / 2, n / 2)
                process(spectrogramListener)
            }
        }
    }

    private fun process(spectrogramListener: SpectrogramListener) {

        val floatFFT = FloatArray(fftBuffer!!.size)
        fftBuffer!!.forEachIndexed { index, sh ->
            floatFFT[index] = sh.toFloat()
        }

        val fft = FloatFFT_1D(fftResolution.toLong())
        val mag = FloatArray(floatFFT.size / 2)
        fft.realForward(floatFFT)
        for (i in 0 until floatFFT.size / 2) {
            val real = floatFFT[2 * i]
            val imagine = floatFFT[2 * i + 1]
            mag[i] = sqrt(real * real + imagine * imagine) / 83886070
        }

        spectrogramListener.onProcessed(mag)
    }
}

interface SpectrogramListener {
    fun onProcessed(mag: FloatArray)
}

fun ShortArray.toSmallChunk(number: Int): List<ShortArray> {
    val numberOfChunk = this.size / number
    val resultChunk = arrayListOf<ShortArray>()
    var i = 0
    while (i < this.size) {
        resultChunk.add(this.copyOfRange(i, min(this.size, i + numberOfChunk)))
        i += numberOfChunk
    }
    return resultChunk
}

fun ByteArray.toShortArray(): ShortArray {
    val shortArray = ShortArray(this.size / 4)
    val byteBuffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    for (i in shortArray.indices) {
        shortArray[i] = byteBuffer.short
    }
    return shortArray
}
