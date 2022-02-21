package org.rfcx.companion.util

import android.content.Context
import android.net.ConnectivityManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import java.net.InetAddress
import android.net.NetworkInfo

import android.net.NetworkCapabilities
import android.os.Build


internal fun Context?.isNetworkAvailable(): Boolean {
    if (this == null) return false
    val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } else {
        val activeNetworkInfo = cm.activeNetworkInfo
        activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting;
    }
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

fun View.hideKeyboard() = this.let {
    val inputManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    this.requestFocus()
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

private val chars = ('A'..'F') + ('0'..'9')
fun randomDeploymentId(): String = List(16) { chars.random() }.joinToString("")
