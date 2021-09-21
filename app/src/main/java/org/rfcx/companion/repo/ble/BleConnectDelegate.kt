package org.rfcx.companion.repo.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.content.Context.BIND_AUTO_CREATE
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import org.rfcx.companion.entity.songmeter.SongMeterConstant
import org.rfcx.companion.util.Resource
import java.util.*

class BleConnectDelegate(private val context: Context) {

    private var deviceAddress: String? = null
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
    var command: BluetoothGattCharacteristic? = null

    var needCheck = false

    val setSiteLiveData = MutableLiveData<Resource<Boolean>>()
    val getConfigLiveData = MutableLiveData<Resource<Boolean>>()

    var requestConfigVersion = 0
    var expectedPrefixes: ByteArray? = null

    var count = 0

    private var gattConnection = MutableLiveData<Resource<Boolean>>()

    private var handler = Handler(Looper.getMainLooper())

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bleConnectService = (service as BleConnectService.LocalBinder).service
            bleConnectService?.let {
                if (!it.initialize()) {
                    //TODO: finish
                }
                it.connect(deviceAddress)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleConnectService = null
        }
    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BleConnectService.ACTION_GATT_CONNECTED -> {
                    gattConnection.postValue(Resource.success(true))
                }
                BleConnectService.ACTION_GATT_DISCONNECTED -> {
                    gattConnection.postValue(Resource.success(false))
                    // Send after close connection
                    if (needCheck) {
                        setSiteLiveData.postValue(Resource.success(true))
                        needCheck = false
                    }
                }
                BleConnectService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    setGattServices(bleConnectService?.supportedGattServices)
                }
                BleConnectService.ACTION_CHA_CHANGE -> {
                    when {
                        intent.getStringExtra(BleConnectService.CHARACTERISTICS)!!
                            .toUpperCase() == SongMeterConstant.kUUIDCharConfigRtoA -> {
                            intent.getByteArrayExtra(BleConnectService.EXTRA_DATA)?.let {
                                manageRtoAChanges(it)
                            }
                        }
                        intent.getStringExtra(BleConnectService.CHARACTERISTICS)!!
                            .toUpperCase() == SongMeterConstant.kUUIDCharStatus -> {
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
        firstTime = false
        getConfigLiveData.postValue(Resource.success(null))
        configRtoAData = changes
        configAtoRData = changes.also {
            it[0] = requestConfigVersion.toByte()
            it[1] = (0).toByte()
        }
        if (needCheck) {
            if (expectedPrefixes != null) {
                val actualPrefixes = changes.copyOfRange(6, 18)
                actualPrefixes.forEachIndexed { index, byte ->
                    if (byte != expectedPrefixes!![index]) {
                        // If not match then fail
                        setSiteLiveData.postValue(Resource.success(false))
                        needCheck = false
                    }
                }
                expectedPrefixes = null
            }
        }
    }

    fun requestChangePrefixes(prefixes: String) {
        configAtoRData?.also {
            setSiteLiveData.postValue(Resource.loading(null))
            expectedPrefixes = prefixes.toByteArray()
            requestConfigVersion++
            it[0] = requestConfigVersion.toByte()
            for (index in 0..11) {
                it[index + 6] = expectedPrefixes?.getOrNull(index) ?: (0).toByte()
            }
            writeCharacteristic(configAtoR, it)
        }
    }

    private fun requestCurrentConfig() {
        if (firstTime) {
            getConfigLiveData.postValue(Resource.loading(null))
            requestConfigVersion++
            val configData = ByteArray(128)
            Arrays.fill(configData, (0).toByte())
            configData[0] = requestConfigVersion.toByte()
            configData[1] = (0).toByte()
            writeCharacteristic(configAtoR, configData)
        }
    }

    private fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic?,
        data: ByteArray?
    ) {
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
                SongMeterConstant.kUUIDCharCommand -> {
                    command = characteristic
                    mapCharacteristic[SongMeterConstant.kUUIDCharCommand] = command!!
                    bleConnectService?.commandChar = command
                }
            }
        }

        handler.post {
            bleConnectService?.requestMTU()
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
        if (configRtoA != null) {
            bleConnectService?.setCharacteristicNotification(configRtoA!!, false)
        }
    }

    companion object {
        var mapCharacteristic: MutableMap<String, BluetoothGattCharacteristic> = mutableMapOf()
    }
}
