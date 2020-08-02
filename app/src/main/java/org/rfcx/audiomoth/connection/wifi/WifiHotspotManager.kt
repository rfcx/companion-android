package org.rfcx.audiomoth.connection.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build

class WifiHotspotManager(private val context: Context) {

    private var wifiManager: WifiManager? = null
    private lateinit var wifiScanReceiver: WifiScanReceiver
    private lateinit var wifiConnectionReceiver: WifiConnectionReceiver
    private var isConnected = false

    fun nearbyHotspot(onWifiListener: OnWifiListener) {
        wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        wifiScanReceiver = WifiScanReceiver(onWifiListener)
        context.registerReceiver(
            wifiScanReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )

        wifiManager!!.startScan()
    }

    fun connectTo(guardian: ScanResult, onWifiListener: OnWifiListener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder().also {
                it.setSsid(guardian.SSID)
                it.setWpa2Passphrase("rfcxrfcx")
            }.build()

            val networkRequest = NetworkRequest.Builder().also {
                it.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                it.setNetworkSpecifier(wifiNetworkSpecifier)
            }.build()

            val connectionManager =
                context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectionManager.requestNetwork(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        onWifiListener.onWifiConnected()
                    }
                })
        } else {
            wifiConnectionReceiver = WifiConnectionReceiver(onWifiListener)
            context.registerReceiver(
                wifiConnectionReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )

            val wifiConfig = WifiConfiguration()
            wifiConfig.SSID = "\"${guardian.SSID}\""
            wifiConfig.preSharedKey = "\"rfcxrfcx\""
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
            wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
            wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
            wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA)

            val netId = wifiManager!!.addNetwork(wifiConfig)
            wifiManager!!.disconnect()
            wifiManager!!.enableNetwork(netId, true)
            wifiManager!!.reconnect()
        }
    }

    fun unRegisterReceiver() {
        try {
            context.unregisterReceiver(wifiScanReceiver)
            context.unregisterReceiver(wifiConnectionReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private inner class WifiScanReceiver(private val onWifiListener: OnWifiListener) :
        BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent!!.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val scanResult = wifiManager!!.scanResults
                val guardianWifiHotspot = scanResult.filter {
                    it.SSID.contains("rfcx")
                }
                if (guardianWifiHotspot.isNotEmpty()) {
                    onWifiListener.onScanReceive(guardianWifiHotspot)
                }
            }
        }
    }

    private inner class WifiConnectionReceiver(private val onWifiListener: OnWifiListener) :
        BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val conManager =
                context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = conManager.activeNetworkInfo
            if (netInfo != null && netInfo.isConnected && netInfo.type == ConnectivityManager.TYPE_WIFI) {
                if (!isConnected) {
                    onWifiListener.onWifiConnected()
                    isConnected = true
                }
            }
        }
    }
}

interface OnWifiListener {
    fun onScanReceive(result: List<ScanResult>)
    fun onWifiConnected()
}
