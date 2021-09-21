package org.rfcx.companion.repo.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.content.Context.BIND_AUTO_CREATE
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import org.rfcx.companion.entity.songmeter.SongMeterConstant
import org.rfcx.companion.util.Resource
import java.util.*

class BleConnectDelegate(private val context: Context) {

    private var deviceAddress: String? = null
    private var isConnected = false
    private var bleConnectService: BleConnectService? = null

    var configService: BluetoothGattService? = null
    var configCharacteristics: List<BluetoothGattCharacteristic>? = null

    var firstTime = true
    var configAtoR: BluetoothGattCharacteristic? = null
    var configAtoRData: ByteArray? = null
    var configRtoA: BluetoothGattCharacteristic? = null
    var configRtoAData: ByteArray? = null
    var schedAtoR: BluetoothGattCharacteristic? = null
    var schedRtoA: BluetoothGattCharacteristic? = null
    var bulkAckAtoR: BluetoothGattCharacteristic? = null
    var bulkDataRtoA: BluetoothGattCharacteristic? = null
    var response: BluetoothGattCharacteristic? = null
    var status: BluetoothGattCharacteristic? = null

    var needCheck = false

    var requestConfigVersion = 0
    var seqAtoRSendVersion = 0
    var seqAtoRRecvVersion = 0

    var count = 0

    var recorder: BluetoothGatt? = null
    private var gattConnection = MutableLiveData<Resource<Boolean>>()

    private var handler = Handler(Looper.getMainLooper())

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
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BleConnectService.ACTION_GATT_CONNECTED -> {
                    isConnected = true
                    gattConnection.postValue(Resource.success(isConnected))
                }
                BleConnectService.ACTION_GATT_DISCONNECTED -> {
                    isConnected = false
                    gattConnection.postValue(Resource.success(isConnected))
                }
                BleConnectService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    setGattServices(bleConnectService?.supportedGattServices)
                }
                BleConnectService.ACTION_CHA_WRITE -> {
                    when {
                        intent.getStringExtra(BleConnectService.CHARACTERISTICS)!!.toUpperCase() == SongMeterConstant.kUUIDCharConfigAtoR -> {
                            handler.post {
//                                bleConnectService?.readCharacteristic(configRtoA)
                            }
                        }
                    }
                }
                BleConnectService.ACTION_CHA_READ -> {
                    displayData(intent.getByteArrayExtra(BleConnectService.EXTRA_DATA))
                    when {
                        intent.getStringExtra(BleConnectService.CHARACTERISTICS)!!.toUpperCase() == SongMeterConstant.kUUIDCharConfigAtoR -> {

                        }
                        intent.getStringExtra(BleConnectService.CHARACTERISTICS)!!.toUpperCase() == SongMeterConstant.kUUIDCharConfigRtoA -> {
                            configRtoAData = intent.getByteArrayExtra(BleConnectService.EXTRA_DATA)
                            handler.post {
//                                bleConnectService?.readCharacteristic(configAtoR)
                            }
                        }
                    }
                }
                BleConnectService.ACTION_CHA_CHANGE -> {
                    when {
                        intent.getStringExtra(BleConnectService.CHARACTERISTICS)!!.toUpperCase() == SongMeterConstant.kUUIDCharConfigRtoA -> {
                            intent.getByteArrayExtra(BleConnectService.EXTRA_DATA)?.let {
                                manageRtoAChanges(it)
                            }
                        }
                        intent.getStringExtra(BleConnectService.CHARACTERISTICS)!!.toUpperCase() == SongMeterConstant.kUUIDCharStatus -> {
                            requestCurrentConfig()
                        }
                    }
                }
                BleConnectService.ACTION_DESCRIPTOR_WRITTEN -> {
                    handler.post {
                        bleConnectService?.readCharacteristic(configAtoR)
                    }
                }
            }
        }
    }

    private fun manageRtoAChanges(changes: ByteArray) {
        configRtoAData = changes
        configAtoRData = changes.also {
            it[0] = requestConfigVersion.toByte()
            it[1] = (0).toByte()
        }
        if (needCheck) {
            needCheck = false
        }
    }

    fun requestChangePrefixes(prefixes: String) {
        configAtoRData?.also {
            val prefixesConverted = prefixes.toByteArray()
            requestConfigVersion++
            it[0] = requestConfigVersion.toByte()
            writeCharacteristic(configAtoR, it)
        }
    }

    private fun requestCurrentConfig() {
        if (firstTime) {
            requestConfigVersion++
            val configData = ByteArray(128)
            Arrays.fill(configData, (0).toByte())
            configData[0] = requestConfigVersion.toByte()
            configData[1] = (0).toByte()
            writeCharacteristic(configAtoR, configData)
            firstTime = false
        }
    }

    private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic?, data: ByteArray?) {
        if (characteristic == null || data == null) {
            return
        }
        handler.post {
            needCheck = true
            characteristic.value = data
            bleConnectService?.writeCharacteristic(characteristic)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        gattServices.forEach { gattService ->
            if (gattService.uuid.toString().toUpperCase() == SongMeterConstant.kUUIDCommsService) {
                configService = gattService
                configCharacteristics = gattService.characteristics
            }
        }
        configCharacteristics?.forEach { characteristic ->
            when (characteristic.uuid.toString().toUpperCase()) {
                SongMeterConstant.kUUIDCharConfigAtoR -> {
                    configAtoR = characteristic
                    mapCharacteristic[SongMeterConstant.kUUIDCharConfigAtoR] = configAtoR!!
                }
                SongMeterConstant.kUUIDCharConfigRtoA -> {
                    configRtoA = characteristic
                    mapCharacteristic[SongMeterConstant.kUUIDCharConfigRtoA] = configRtoA!!
                }
                SongMeterConstant.kUUIDCharSchedAtoR -> {
                    schedAtoR = characteristic
                    mapCharacteristic[SongMeterConstant.kUUIDCharSchedAtoR] = schedAtoR!!
                }
                SongMeterConstant.kUUIDCharSchedRtoA -> {
                    schedRtoA = characteristic
                    mapCharacteristic[SongMeterConstant.kUUIDCharSchedRtoA] = schedRtoA!!
                }
                SongMeterConstant.kUUIDCharBulkAckAtoR -> {
                    bulkAckAtoR = characteristic
                    mapCharacteristic[SongMeterConstant.kUUIDCharBulkAckAtoR] = bulkAckAtoR!!
                }
                SongMeterConstant.kUUIDCharBulkDataRtoA -> {
                    bulkDataRtoA = characteristic
                    mapCharacteristic[SongMeterConstant.kUUIDCharBulkDataRtoA] = bulkDataRtoA!!
                }
                SongMeterConstant.kUUIDCharResponse -> {
                    response = characteristic
                    mapCharacteristic[SongMeterConstant.kUUIDCharResponse] = response!!
                }
                SongMeterConstant.kUUIDCharStatus -> {
                    status = characteristic
                    mapCharacteristic[SongMeterConstant.kUUIDCharStatus] = status!!
                }
            }
        }

        Log.d("BLE", "setting noti")
        handler.post {
            bleConnectService?.requestMTU()
        }
    }

    private fun displayData(data: ByteArray?) {
        if (data == null) return
        Log.d("BLE", data.contentToString())
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
        intentFilter.addAction(BleConnectService.ACTION_CHA_WRITE)
        intentFilter.addAction(BleConnectService.ACTION_CHA_READ)
        intentFilter.addAction(BleConnectService.ACTION_CHA_CHANGE)
        intentFilter.addAction(BleConnectService.ACTION_DESCRIPTOR_WRITTEN)
        return intentFilter
    }

    fun observeGattConnection() = gattConnection

    fun registerReceiver() {
        context.registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        bleConnectService?.connect(deviceAddress)
    }

    fun unRegisterReceiver() {
        context.unregisterReceiver(gattUpdateReceiver)
    }

    fun bindService(address: String) {
        val gattServiceIntent = Intent(context, BleConnectService::class.java)
        deviceAddress = address
        context.bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    fun unbindService() {
        context.unbindService(serviceConnection)
    }

    companion object {
        var mapCharacteristic: MutableMap<String, BluetoothGattCharacteristic> = mutableMapOf()
    }
}
