package org.rfcx.companion.connection.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.annotation.RequiresApi
import org.rfcx.companion.util.WifiHotspotUtils

class WifiHotspotManager(private val context: Context) {

    private var wifiManager: WifiManager? = null
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var wifiScanReceiver: WifiScanReceiver
    private lateinit var wifiConnectionReceiver: WifiConnectionReceiver
    private var isConnected = false
    private var isRegisteredCallback = false

    private var wifiName = ""

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
        wifiName = guardian.SSID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder().also {
                it.setSsid(guardian.SSID)
                it.setWpa2Passphrase("rfcxrfcx")
            }.build()

            val networkRequest = NetworkRequest.Builder().also {
                it.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                it.setNetworkSpecifier(wifiNetworkSpecifier)
            }.build()

            connectivityManager =
                context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.requestNetwork(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        println("available")
                        connectivityManager.bindProcessToNetwork(network)
                        onWifiListener.onWifiConnected()
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        println("lost")
                        connectivityManager.bindProcessToNetwork(null)
                    }

                    override fun onUnavailable() {
                        super.onUnavailable()
                        println("unavailable")
                    }
                }
            )
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

            wifiManager!!.disconnect()
            val netId = wifiManager!!.addNetwork(wifiConfig)
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

    fun unRegisterWifiLost() {
        unregisterWifiConnectionLost()
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
                    if (WifiHotspotUtils.isConnectedWithGuardian(context, wifiName)) {
                        onWifiListener.onWifiConnected()
                        wifiName = ""
                        isConnected = true
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private inner class WifiLostCallback(private val wifiLostListener: WifiLostListener) : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            super.onLost(network)
            wifiLostListener.onLost()
        }
    }

    fun registerWifiConnectionLost(wifiLostListener: WifiLostListener) {
        if (isRegisteredCallback) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest = NetworkRequest.Builder().also {
                it.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            }.build()
            networkCallback = WifiLostCallback(wifiLostListener)
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            isRegisteredCallback = true
        }
    }

    private fun unregisterWifiConnectionLost() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isRegisteredCallback) {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                isRegisteredCallback = false
            }
        }
    }
}

interface OnWifiListener {
    fun onScanReceive(result: List<ScanResult>)
    fun onWifiConnected()
}

interface WifiLostListener {
    fun onLost()
}
