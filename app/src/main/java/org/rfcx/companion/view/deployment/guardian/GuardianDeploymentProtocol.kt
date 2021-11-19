package org.rfcx.companion.view.deployment.guardian

import com.google.gson.JsonObject
import org.rfcx.companion.connection.wifi.WifiHotspotManager
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol

interface GuardianDeploymentProtocol : BaseDeploymentProtocol {
    fun startSetupConfigure()
    fun startGuardianRegister()
    fun backToConfigure()

    fun getDeployment(): Deployment?
    fun getSampleRate(): Int
    fun getLastCheckInTime(): Long?
    fun getGuid(): String?
    fun isGuardianRegistered(): Boolean?
    fun getSoftwareVersion(): Map<String, String>?
    fun getAudioConfiguration(): JsonObject?
    fun getPrefsSha1(): String?
    fun getLatestCheckIn(): JsonObject?

    fun getNetwork(): Int?
    fun getSwmNetwork(): Int?
    fun getSentinelPower(): String?

    fun setDeployment(deployment: Deployment)
    fun setSampleRate(sampleRate: Int)
    fun setOnDeployClicked()
    fun setLastCheckInTime(time: Long?)

    fun addRegisteredToPassedCheck()
    fun removeRegisteredOnPassedCheck()

    fun showConnectInstruction()

    fun showLoading()
    fun hideLoading()

    fun setWifiManager(wifiManager: WifiHotspotManager)
    fun registerWifiConnectionLostListener()
    fun unregisterWifiConnectionLostListener()
}
