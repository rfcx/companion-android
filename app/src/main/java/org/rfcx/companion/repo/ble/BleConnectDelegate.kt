package org.rfcx.companion.repo.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.content.Context.BIND_AUTO_CREATE
import android.os.IBinder
import android.util.Log
import org.rfcx.companion.entity.songmeter.SongMeterConstant

class BleConnectDelegate(private val context: Context) {

    private var deviceAddress: String? = null
    private var isConnected = false
    private var bleConnectService: BleConnectService? = null

    var configService: BluetoothGattService? = null
    var configCharacteristics: List<BluetoothGattCharacteristic>? = null

    var configAtoR: BluetoothGattCharacteristic? = null
    var configRtoA: BluetoothGattCharacteristic? = null

    var recorder: BluetoothGatt? = null

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
                    setGattServices(bleConnectService?.supportedGattServices)
                }
                BleConnectService.ACTION_DATA_AVAILABLE -> {

                }
            }
        }
    }

    private fun setGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        gattServices.forEach { gattService ->
            if (gattService.uuid.toString() == SongMeterConstant.kUUIDCommsService) {
                configService = gattService
                configCharacteristics = gattService.characteristics
            }
        }
        configCharacteristics?.forEach { characteristic ->
            when (characteristic.uuid.toString()) {
                SongMeterConstant.kUUIDCharConfigAtoR -> configAtoR = characteristic
                SongMeterConstant.kUUIDCharConfigRtoA -> configRtoA = characteristic
            }
            bleConnectService?.readCharacteristic(characteristic)
        }
    }

    private fun setSiteId(id: String) {
        configAtoR?.let {
            val data = it.value
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
