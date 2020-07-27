package org.rfcx.audiomoth.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build

object WifiHotspotUtils {

    fun isConnectedWithGuardian(context: Context, name: String): Boolean {
        return name == getCurrentWifiName(context)
    }

    private fun getCurrentWifiName(context: Context): String {
        var wifiName = ""
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm?.run {
                cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                    if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        wifiName = getSSID(context)
                    }
                }
            }
        } else {
            cm?.run {
                cm.activeNetworkInfo?.run {
                    if (type == ConnectivityManager.TYPE_WIFI) {
                        wifiName = getSSID(context)
                    }
                }
            }
        }

        return wifiName
    }

    private fun getSSID(context: Context): String {
        var ssid = ""
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionInfo = wifiManager.connectionInfo
        if (connectionInfo != null && !connectionInfo.ssid.isBlank()) {
            ssid = connectionInfo.ssid
        }
        return ssid.replace("\"", "")
    }
}
