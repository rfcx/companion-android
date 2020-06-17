package org.rfcx.audiomoth.view.deployment.guardian

import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration
import org.rfcx.audiomoth.entity.guardian.GuardianProfile
import org.rfcx.audiomoth.view.deployment.BaseDeploymentProtocal

interface GuardianDeploymentProtocol : BaseDeploymentProtocal {
    fun startSetupConfigure(profile: GuardianProfile)
    fun startSyncing(status: String)
    fun setDeploymentConfigure()

    fun getProfiles(): List<GuardianProfile>
    fun getProfile(): GuardianProfile?

    fun setProfile(profile: GuardianProfile)
}
