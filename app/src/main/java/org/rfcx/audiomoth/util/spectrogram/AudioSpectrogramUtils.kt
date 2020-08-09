package org.rfcx.audiomoth.util.spectrogram

import androidx.lifecycle.MutableLiveData
import org.jtransforms.fft.FloatFFT_1D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt


object AudioSpectrogramUtils {

    var fftResolution = 1024

    private var bufferStack: ArrayList<ShortArray>? = null
    private var fftBuffer: ShortArray? = null
    private var isSetup = false

    val spectrogramLive = MutableLiveData<FloatArray>()

    init {
        spectrogramLive.value = FloatArray(1)
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

    fun getTrunks(recordBuffer: ShortArray) {
        val n = fftResolution
        if(bufferStack != null) {
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
                process()
            }

            // Last item has not yet fully be used (only its first half)
            // Move it to first position in arraylist so that its last half is used
            val first: ShortArray = bufferStack!![0]
            val last: ShortArray = bufferStack!![bufferStack!!.size - 1]
            System.arraycopy(last, 0, first, 0, n / 2)
        }
    }

    private fun process() {

        val floatFFT = FloatArray(fftBuffer!!.size)
        fftBuffer!!.forEachIndexed { index, sh ->
            floatFFT[index] = sh.toFloat()
        }

        val fft = FloatFFT_1D(fftResolution.toLong())
        val mag = FloatArray(floatFFT.size/2)
        fft.realForward(floatFFT)
        for (i in 0 until floatFFT.size/2) {
            val real = floatFFT[2 * i]
            val imagine = floatFFT[2 * i + 1]
            mag[i] = sqrt(real * real + imagine * imagine) / 83886070
        }
        spectrogramLive.value = mag
    }
}

fun ByteArray.toShortArray(): ShortArray {
    val shortArray = ShortArray(this.size / 4)
    val byteBuffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    for (i in shortArray.indices) {
        shortArray[i] = byteBuffer.short
    }
    return shortArray
}
