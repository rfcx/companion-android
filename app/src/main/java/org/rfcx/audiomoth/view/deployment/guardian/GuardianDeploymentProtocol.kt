package org.rfcx.audiomoth.view.deployment.guardian

import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration
import org.rfcx.audiomoth.entity.guardian.GuardianDeployment
import org.rfcx.audiomoth.entity.guardian.GuardianProfile
import org.rfcx.audiomoth.view.deployment.BaseDeploymentProtocal

interface GuardianDeploymentProtocol : BaseDeploymentProtocal {
    fun startSetupConfigure(profile: GuardianProfile)

    fun backToConfigure()

    fun getProfiles(): List<GuardianProfile>
    fun getProfile(): GuardianProfile?
    fun getDeployment(): GuardianDeployment?
    fun getConfiguration(): GuardianConfiguration?

    fun setProfile(profile: GuardianProfile)
    fun setDeploymentConfigure(profile: GuardianProfile)
    fun setDeployment(deployment: GuardianDeployment)
    fun setDeploymentWifiName(name: String)

    fun showLoading()
    fun hideLoading()
}
