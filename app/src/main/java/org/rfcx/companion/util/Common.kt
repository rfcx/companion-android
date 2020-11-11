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

private val chars = ('A'..'F') + ('0'..'9')
private val numbers = (0..9)
fun randomDeploymentId(): String = List(16) { chars.random() }.joinToString("")
fun randomDeploymentIdOnlyNumber(): String = List(8) { numbers.random() }.joinToString("")
