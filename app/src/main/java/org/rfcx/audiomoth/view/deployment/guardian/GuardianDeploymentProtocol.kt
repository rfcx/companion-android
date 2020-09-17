package org.rfcx.audiomoth.view.deployment.guardian

import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration
import org.rfcx.audiomoth.entity.guardian.GuardianDeployment
import org.rfcx.audiomoth.entity.guardian.GuardianProfile
import org.rfcx.audiomoth.view.deployment.BaseDeploymentProtocol

interface GuardianDeploymentProtocol : BaseDeploymentProtocol {
    fun startSetupConfigure(profile: GuardianProfile)
    fun startCheckList()

    fun backToConfigure()

    fun getProfiles(): List<GuardianProfile>
    fun getProfile(): GuardianProfile?
    fun getDeployment(): GuardianDeployment?
    fun getConfiguration(): GuardianConfiguration?
    fun getSampleRate(): Int
    fun getWifiName(): String

    fun setReadyToDeploy()

    fun setProfile(profile: GuardianProfile)
    fun setDeploymentConfigure(profile: GuardianProfile)
    fun setDeployment(deployment: GuardianDeployment)
    fun setDeploymentWifiName(name: String)
    fun setSampleRate(sampleRate: Int)
    fun setImages(images: List<String>)

    fun handleCheckClicked(number: Int)
    fun getPassedChecks(): List<Int>

    fun showLoading()
    fun hideLoading()
}
