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


/**
 * Class associated with the spectrogram view
 * Handles events:
 * onSizeChanged, onTouchEvent, onDraw
 */

package org.rfcx.audiomoth.view.deployment.guardian.microphone

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class FrequencyView : View {
    // Attributes
    private var activity: Activity
    private val paint = Paint()
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var pos = 0
    private var samplingRate = 0
    private var _width = 0
    private var _height = 100
    private var _magnitudes: DoubleArray? = null
    private val colorRainbow =
        intArrayOf(-0x1, -0xff01, -0x10000, -0x100, -0xff0100, -0xff0001, -0xffff01, -0x1000000)
    private val colorFire = intArrayOf(-0x1, -0x100, -0x10000, -0x1000000)
    private val colorIce = intArrayOf(-0x1, -0xff0001, -0xffff01, -0xfffff1, -0x1000000)
    private val colorGrey = intArrayOf(-0x1, -0x1000000)

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
        bitmap = Bitmap.createBitmap(_width, _height*4, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap!!)
    }

    fun setSamplingRate(sampling: Int) {
        samplingRate = sampling
    }

    fun setMagnitudes(m: DoubleArray) {
        _magnitudes = DoubleArray(m.size)
        System.arraycopy(m, 0, _magnitudes, 0, m.size)
    }

    /**
     * Called whenever a redraw is needed
     * Renders spectrogram and scale on the right
     * Frequency scale can be linear or logarithmic
     */
    public override fun onDraw(canvas: Canvas) {
        var colors: IntArray? = null
        val colorScale = "Ice"

        when (colorScale) {
            "Grey" -> colors = colorGrey
            "Fire" -> colors = colorFire
            "Ice" -> colors = colorIce
            "Rainbow" -> colors = colorRainbow
        }
        val wColor = 10
        val wFrequency = 40
        val rWidth = _width - wColor - wFrequency
        paint.strokeWidth = 1f

        // Get scale preferences
        val logFrequency = false

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
                samplingRate.toFloat(),
                logFrequency
            )
            j /= samplingRate.toFloat()
            if (_magnitudes != null) {
                val mag = _magnitudes!![(j * _magnitudes!!.size / 2).toInt()]
                val db =
                    Math.max(0.0, -20 * Math.log10(mag.toDouble())).toFloat()
                val c = getInterpolatedColor(colors, db * 0.009f)
                paint.color = c
                val x = pos % rWidth
                this.canvas!!.drawPoint(x.toFloat(), i.toFloat(), paint)
            }
        }

        // Draw bitmap
        if (pos < rWidth) {
            canvas.drawBitmap(bitmap!!, wColor.toFloat(), 0f, paint)
        } else {
            canvas.drawBitmap(bitmap!!, wColor.toFloat() - pos % rWidth, 0f, paint)
            canvas.drawBitmap(bitmap!!, wColor.toFloat() + (rWidth - pos % rWidth), 0f, paint)
        }
        pos++
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
        return if (log) (Math.pow(
            10.0,
            position * Math.log10(1 + maxValue - minValue.toDouble())
        ) + minValue - 1).toFloat() else minValue + position * (maxValue - minValue)
    }

    /**
     * Calculate rainbow colors
     */
    private fun ave(s: Int, d: Int, p: Float): Int {
        return s + Math.round(p * (d - s))
    }

    private fun getInterpolatedColor(colors: IntArray?, unit: Float): Int {
        if (unit <= 0) return colors!![0]
        if (unit >= 1) return colors!![colors.size - 1]
        var p = unit * (colors!!.size - 1)
        val i = p.toInt()
        p -= i.toFloat()

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
