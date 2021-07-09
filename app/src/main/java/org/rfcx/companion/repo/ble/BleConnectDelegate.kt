package org.rfcx.companion.repo.ble

import android.content.*
import android.content.Context.BIND_AUTO_CREATE
import android.os.IBinder
import android.util.Log

class BleConnectDelegate(private val context: Context) {

    private var deviceAddress: String? = null
    private var isConnected = false
    private var bleConnectService: BleConnectService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("BLE", "connected")
            bleConnectService = (service as BleConnectService.LocalBinder).service
            bleConnectService?.let {
                if (!it.initialize()) {
                    //TODO: finish
                }
                it.connect(deviceAddress)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("BLE", "disconnected")
            bleConnectService = null
        }
    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BleConnectService.ACTION_GATT_CONNECTED -> {
                    isConnected = true
                    Log.d("BLE", "connect")
                }
                BleConnectService.ACTION_GATT_DISCONNECTED -> {
                    isConnected = false
                    Log.d("BLE", "disconnect")
                }
                BleConnectService.ACTION_GATT_SERVICES_DISCOVERED -> {

                }
                BleConnectService.ACTION_DATA_AVAILABLE -> {

                }
            }
        }
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BleConnectService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BleConnectService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BleConnectService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BleConnectService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }

    fun registerReceiver() {
        context.registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        bleConnectService?.connect(deviceAddress)
    }

    fun unRegisterReceiver() {
        context.unregisterReceiver(gattUpdateReceiver)
    }

    fun bindService(address: String) {
        val gattServiceIntent = Intent(context, BleConnectService::class.java)
        context.bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE)
        deviceAddress = address
    }

    fun unbindService() {
        context.unbindService(serviceConnection)
    }

}
