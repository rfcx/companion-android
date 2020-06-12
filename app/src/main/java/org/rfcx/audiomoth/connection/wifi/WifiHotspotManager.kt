package org.rfcx.audiomoth.connection.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager

class WifiHotspotManager(private val context: Context) {

    private var wifiManager: WifiManager? = null

    fun nearbyHotspot(onScanReceiver: OnScanReceiver) {
        wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        context.registerReceiver(
            WifiScanReceiver(onScanReceiver),
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )

        wifiManager!!.startScan()
    }

    fun unRegisterReceiver() {
        context.unregisterReceiver(WifiScanReceiver(null))
    }

    inner class WifiScanReceiver(private val onScanReceiver: OnScanReceiver?) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent!!.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val scanResult = wifiManager!!.scanResults
                val guardianWifiHotspot = scanResult.filter {
                    it.SSID.contains("rfcx")
                }
                onScanReceiver?.onReceive(guardianWifiHotspot)
            }
        }
    }
}

interface OnScanReceiver {
    fun onReceive(result: List<ScanResult>)
}
