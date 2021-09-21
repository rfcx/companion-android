package org.rfcx.companion.repo.ble

import androidx.lifecycle.LiveData

class BleHelper(private val detectService: BleDetectService, private val connectService: BleConnectDelegate) {
    fun scanBle(isEnabled: Boolean) {
        detectService.scanBle(isEnabled)
    }

    fun stopScanBle() {
        detectService.stopScanBle()
    }

    fun observeAdvertisement() = detectService.getAdvertisement()

    fun observeGattConnection() = connectService.observeGattConnection()

    fun registerGattReceiver() {
        connectService.registerReceiver()
    }

    fun unRegisterGattReceiver() {
        connectService.unRegisterReceiver()
    }

    fun bindConnectService(address: String) {
        connectService.bindService(address)
    }

    fun unBindConnectService() {
        connectService.unbindService()
    }

    fun getSetSiteLiveData() = connectService.setSiteLiveData

    fun getRequestConfigLiveData() = connectService.getConfigLiveData

    fun setPrefixes(prefixes: String) {
        connectService.requestChangePrefixes(prefixes)
    }
}
