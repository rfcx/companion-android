package org.rfcx.audiomoth.util

import android.content.Context
import androidx.core.content.ContextCompat

fun Context.getIntColor(res: Int): Int {
    return ContextCompat.getColor(this, res)
}