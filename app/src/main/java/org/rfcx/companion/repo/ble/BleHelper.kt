package org.rfcx.companion.repo.ble

class BleHelper(private val detectService: BleDetectService) {
    fun scanBle(isEnabled: Boolean) {
        detectService.scanBle(isEnabled)
    }

    fun stopScanBle() {
        detectService.stopScanBle()
    }

    fun observeAdvertisement() = detectService.getAdvertisement()
}
