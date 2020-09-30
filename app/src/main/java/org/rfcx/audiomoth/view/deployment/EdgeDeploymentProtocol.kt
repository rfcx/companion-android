package org.rfcx.audiomoth.view.deployment

import java.sql.Timestamp
import org.rfcx.audiomoth.entity.*

interface EdgeDeploymentProtocol : BaseDeploymentProtocol {
    fun openWithEdgeDevice()
    fun openWithGuardianDevice()

    fun startSyncing(status: String)

    fun getProfiles(): List<Profile>
    fun getProfile(): Profile?
    fun getDeployment(): EdgeDeployment?
    fun geConfiguration(): EdgeConfiguration?

    fun setDeployment(deployment: EdgeDeployment)
    fun setProfile(profile: Profile)
    fun setDeploymentConfigure(profile: Profile)
    fun setPerformBattery(batteryDepletedAt: Timestamp, batteryLevel: Int)

    fun playSyncSound()
}
