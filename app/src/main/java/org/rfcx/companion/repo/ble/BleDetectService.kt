package org.rfcx.companion.repo.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import org.rfcx.companion.entity.songmeter.Advertisement
import org.rfcx.companion.util.Resource
import org.rfcx.companion.util.songmeter.AdvertisementUtils

class BleDetectService(context: Context) {
    private val bluetoothAdapter: BluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private var isScanning = false

    private var serialNumber: String? = null
    private var prefixes: String? = null
    private var address: String? = null
    private var readyToPair: Boolean? = null
    private val advertisementUtils by lazy { AdvertisementUtils() }

    private var advertisement = MutableLiveData<Resource<List<Advertisement>>>()
    private val advertisements = mutableListOf<Advertisement>()

    private val scannerCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : ScanCallback() {

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            if (result != null) {
                if (result.device.address.split(":").joinToString("").substring(0, 6) == "9C25BE") {
                    address = result.device.address
                    advertisementUtils.convertAdvertisementToObject(result.scanRecord!!.bytes)
                    serialNumber = advertisementUtils.getSerialNumber()
                    prefixes = advertisementUtils.getPrefixes()
                    readyToPair = advertisementUtils.getReadyToPair()
                    updateAdvertisement()
                }
            }
        }
    }

    fun clear() {
        serialNumber = null
        prefixes = null
        address = null
        readyToPair = null
        advertisementUtils.clear()
    }

    fun updateAdvertisement() {
        if (prefixes != null && serialNumber != null && readyToPair != null) {
            val advm = advertisements.find { it.serialName == "SMM${serialNumber!!}" }
            if (advm == null) {
                advertisements.add(Advertisement(prefixes!!, "SMM${serialNumber!!}", address!!, readyToPair!!))
            } else {
                advm.isReadyToPair = readyToPair!!
                if (readyToPair == false) {
                    advm.prefixes = prefixes!!
                }
            }
            advertisement.postValue(Resource.success(advertisements))
        } else if (prefixes != null || serialNumber != null || readyToPair != null) {
            advertisement.postValue(Resource.loading(null))
        } else {
            advertisement.postValue(Resource.success(null))
        }
    }

    fun getAdvertisement() = advertisement

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }

    fun scanBle(isEnable: Boolean) {
        if (isBluetoothEnabled()) {
            val scanner = bluetoothAdapter.bluetoothLeScanner

            if (isEnable) {
                advertisement.postValue(Resource.loading(null))
                isScanning = true
                scanner.startScan(scannerCallback)
            } else {
                isScanning = false
                scanner.stopScan(scannerCallback)
            }
        }
    }

    fun stopScanBle() {
        scanBle(false)
    }
}
