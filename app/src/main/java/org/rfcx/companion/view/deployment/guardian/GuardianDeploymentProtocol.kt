package org.rfcx.companion.view.deployment.guardian

import org.rfcx.companion.connection.wifi.WifiHotspotManager
import org.rfcx.companion.entity.guardian.GuardianConfiguration
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.entity.guardian.GuardianProfile
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol

interface GuardianDeploymentProtocol : BaseDeploymentProtocol {
    fun startSetupConfigure(profile: GuardianProfile)

    fun backToConfigure()

    fun getProfiles(): List<GuardianProfile>
    fun getProfile(): GuardianProfile?
    fun getDeployment(): GuardianDeployment?
    fun getConfiguration(): GuardianConfiguration?
    fun getSampleRate(): Int
    fun getWifiName(): String

    fun setProfile(profile: GuardianProfile)
    fun setDeploymentConfigure(profile: GuardianProfile)
    fun setDeployment(deployment: GuardianDeployment)
    fun setDeploymentWifiName(name: String)
    fun setSampleRate(sampleRate: Int)

    fun addRegisteredToPassedCheck()
    fun removeRegisteredOnPassedCheck()

    fun showConnectInstruction()

    fun showLoading()
    fun hideLoading()

    fun setWifiManager(wifiManager: WifiHotspotManager)
    fun registerWifiConnectionLostListener()
    fun unregisterWifiConnectionLostListener()
}
