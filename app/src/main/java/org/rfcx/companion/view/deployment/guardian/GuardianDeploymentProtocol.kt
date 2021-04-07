package org.rfcx.companion.view.deployment.guardian

import org.rfcx.companion.connection.wifi.WifiHotspotManager
import org.rfcx.companion.entity.guardian.GuardianConfiguration
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol

interface GuardianDeploymentProtocol : BaseDeploymentProtocol {
    fun startSetupConfigure()

    fun backToConfigure()

    fun getDeployment(): GuardianDeployment?
    fun getConfiguration(): GuardianConfiguration?
    fun getSampleRate(): Int
    fun getWifiName(): String
    fun getLastCheckInTime(): Long?

    fun setDeploymentConfigure(config: GuardianConfiguration)
    fun setDeployment(deployment: GuardianDeployment)
    fun setDeploymentWifiName(name: String)
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
