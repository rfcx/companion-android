package org.rfcx.audiomoth.util

import org.jtransforms.fft.DoubleFFT_1D

object AudioSpectrogramUtils {

    private var window: DoubleArray? = null

    fun extractMagnitude(doubleArray: DoubleArray): DoubleArray {
        val array = doubleArray
        val dfft = DoubleFFT_1D(array.size.toLong())

        System.arraycopy(applyWindow(doubleArray), 0, array, 0, doubleArray.size)
        dfft.realForward(array)

        val mag = DoubleArray(array.size / 2)
        for (i in 0 until array.size / 2) {
            val real = array[2 * i]
            val imagine = array[2 * i + 1]
            mag[i] = real * real + imagine * imagine
        }

        return mag
    }

    private fun buildHammingWindow(size: Int) {
        if (window != null && window!!.size == size) {
            return
        }
        window = DoubleArray(size)
        for (i in 0 until size) {
            window!![i] = .54 - .46 * Math.cos(2 * Math.PI * i / (size - 1.0))
        }
    }

    private fun applyWindow(input: DoubleArray): DoubleArray {
        val res = DoubleArray(input.size)
        buildHammingWindow(input.size)
        for (i in input.indices) {
            res[i] = input[i] * window!![i]
        }
        return res
    }
}

fun ByteArray.toDoubleArray(): DoubleArray {
    val doubleArray = DoubleArray(this.size)
    this.forEachIndexed { index, byte ->
        doubleArray[index] = byte / 32768.0
    }
    return doubleArray
}
