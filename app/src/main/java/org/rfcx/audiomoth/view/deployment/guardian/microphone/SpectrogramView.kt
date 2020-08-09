/**
 * Most of the logic code were from Spectrogram Android application
 */

/**
 * Spectrogram Android application
 * Copyright (c) 2013 Guillaume Adam  http://www.galmiza.net/
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from the use of this software.
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it freely,
 * subject to the following restrictions:
 * 1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software. If you use this software in a product, an acknowledgment in the product documentation would be appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package org.rfcx.audiomoth.view.deployment.guardian.microphone

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import org.rfcx.audiomoth.util.spectrogram.AudioSpectrogramUtils
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

class FrequencyView : View {

    companion object {
        private val colorRainbow =
            intArrayOf(
                -0x1, -0xff01, -0x10000, -0x100, -0xff0100, -0xff0001, -0xffff01, -0x1000000
            )
        private val colorFire =
            intArrayOf(-0x1, -0x100, -0x10000, -0x1000000)
        private val colorIce =
            intArrayOf(-0x1, -0xff0001, -0xffff01, -0x1000000)
        private val colorGrey = intArrayOf(-0x1, -0x1000000)

        private const val RAINBOW = "Rainbow"
        private const val LINEAR = "Linear"
    }
    // Attributes
    private var activity: Activity
    private val paint = Paint()
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var pos = 0
    private var samplingRate = 0
    private var _width = 0
    private var _height = 0
    private var _magnitudes: FloatArray? = null

    var colorScale = RAINBOW
    var freqScale = LINEAR

    constructor(context: Context?) : super(context) {
        activity = context as Activity
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        activity = context as Activity
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        _width = w
        _height = h
        if (bitmap != null) bitmap!!.recycle()
        bitmap = Bitmap.createBitmap(_width, _height, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap!!)
    }

    fun setSamplingRate(sampling: Int) {
        samplingRate = sampling
    }

    fun setMagnitudes(m: FloatArray) {
        _magnitudes = FloatArray(AudioSpectrogramUtils.fftResolution)
        System.arraycopy(m, 0, _magnitudes!!, 0, m.size)
    }

    /**
     * Called whenever a redraw is needed
     * Renders spectrogram and scale on the right
     * Frequency scale can be linear or logarithmic
     */
    public override fun onDraw(canvas: Canvas) {
        var colors: IntArray? = null

        when (colorScale) {
            "Grey" -> colors = colorGrey
            "Fire" -> colors = colorFire
            "Ice" -> colors = colorIce
            "Rainbow" -> colors = colorRainbow
        }
        val wColor = 10
        val wFrequency = 30
        val rWidth = _width - wColor - wFrequency
        paint.strokeWidth = 1f

        // Get scale preferences
        val logFrequency = freqScale != LINEAR

        // Update buffer bitmap
        paint.color = Color.BLACK
        this.canvas!!.drawLine(
            pos % rWidth.toFloat(),
            0f,
            pos % rWidth.toFloat(),
            _height.toFloat(),
            paint
        )
        for (i in 0 until _height) {
            var j = getValueFromRelativePosition(
                (_height - i).toFloat() / _height,
                1f,
                samplingRate.toFloat()/2,
                logFrequency
            )
            j /= samplingRate.toFloat()/2
            if (_magnitudes != null) {
                val mag = _magnitudes!![(j * _magnitudes!!.size / 2).toInt()]
                val db =
                    max(0.0, -20 * log10(mag.toDouble())).toFloat()
                val c = getInterpolatedColor(colors, db * 0.009f)
                paint.color = c
                val x = pos % rWidth
                val y = i
                this.canvas!!.drawPoint(x.toFloat(), y.toFloat(), paint)
                this.canvas!!.drawPoint(x.toFloat(), y.toFloat(), paint)
            }
        }

        // Draw bitmap
        if (pos < rWidth) {
            canvas.drawBitmap(bitmap!!, wColor.toFloat(), 0f, paint)
        } else {
            canvas.drawBitmap(bitmap!!, wColor.toFloat() - pos % rWidth, 0f, paint)
            canvas.drawBitmap(bitmap!!, wColor.toFloat() + (rWidth - pos % rWidth), 0f, paint)
        }

        // Draw color scale
        paint.color = Color.BLACK
        canvas.drawRect(0f, 0f, wColor.toFloat(), height.toFloat(), paint)
        for (i in 0 until height) {
            val c = getInterpolatedColor(colors, i.toFloat() / height)
            paint.color = c
            canvas.drawLine(0f, i.toFloat(), wColor - 5.toFloat(), i.toFloat(), paint)
        }

        // Draw frequency scale
        val ratio = 0.7f * resources.displayMetrics.density
        paint.textSize = 12f * ratio
        paint.color = Color.BLACK
        canvas.drawRect(rWidth + wColor.toFloat(), 0f, width.toFloat(), height.toFloat(), paint)
        paint.color = Color.WHITE
        canvas.drawText("kHz", rWidth + wColor.toFloat(), 12 * ratio, paint)
        if (logFrequency) {
            for (i in 1..4) {
                val y: Float = getRelativePosition(
                    Math.pow(10.0, i.toDouble()).toFloat(),
                    1f,
                    samplingRate / 2.toFloat(),
                    logFrequency
                )
                canvas.drawText("1e$i", rWidth + wColor.toFloat(), (1f - y) * height, paint)
            }
        } else {
            var i = 0
            while (i < (samplingRate - 500) / 2) {
                canvas.drawText(
                    " " + i / 1000,
                    rWidth + wColor.toFloat(),
                    height * (1f - i.toFloat() / (samplingRate / 2)),
                    paint
                )
                i += 1000
            }
        }
        pos++
    }

    /**
     * Converts relative position of a value within given boundaries
     * Log=true for logarithmic scale
     */
    private fun getRelativePosition(
        value: Float,
        minValue: Float,
        maxValue: Float,
        log: Boolean
    ): Float {
        return if (log) log10(1 + value - minValue.toDouble()).toFloat() / Math.log10(
            1 + maxValue - minValue.toDouble()
        ).toFloat() else (value - minValue) / (maxValue - minValue)
    }

    /**
     * Returns a value from its relative position within given boundaries
     * Log=true for logarithmic scale
     */
    private fun getValueFromRelativePosition(
        position: Float,
        minValue: Float,
        maxValue: Float,
        log: Boolean
    ): Float {
        return if (log) (10.0.pow(position * log10(1 + maxValue - minValue.toDouble())) + minValue - 1).toFloat() else minValue + position * (maxValue - minValue)
    }

    /**
     * Calculate rainbow colors
     */
    private fun ave(s: Int, d: Int, p: Float): Int {
        return s + (p * (d - s)).roundToInt()
    }

    private fun getInterpolatedColor(colors: IntArray?, unit: Float): Int {
        if (unit <= 0) return colors!![0]
        if (unit >= 1) return colors!![colors.size - 1]
        var p = unit * (colors!!.size - 1)
        val i = p.toInt()
        p -= i
        // now p is just the fractional part [0...1) and i is the index
        val c0 = colors[i]
        val c1 = colors[i + 1]
        val a = ave(Color.alpha(c0), Color.alpha(c1), p)
        val r = ave(Color.red(c0), Color.red(c1), p)
        val g = ave(Color.green(c0), Color.green(c1), p)
        val b = ave(Color.blue(c0), Color.blue(c1), p)
        return Color.argb(a, r, g, b)
    }
}
