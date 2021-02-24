package org.rfcx.companion.util

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat

internal fun Context?.isNetworkAvailable(): Boolean {
    if (this == null) return false
    val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetworkInfo
    if (activeNetwork != null) {
        return activeNetwork.isConnected
    }
    return false
}

fun Context.getIntColor(res: Int): Int {
    return ContextCompat.getColor(this, res)
}

fun Double.setFormatLabel(): String {
    return if (this >= 1000) "${String.format("%.1f", this/1000)}km" else "${String.format("%.2f", this)}m"
}

fun Float.setFormatLabel(): String {
    return if (this >= 1000) "${String.format("%.1f", this/1000)}km" else "${String.format("%.0f", this)}m"
}

private val chars = ('A'..'F') + ('0'..'9')
fun randomDeploymentId(): String = List(16) { chars.random() }.joinToString("")
