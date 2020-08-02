package org.rfcx.audiomoth.view.deployment

import java.sql.Timestamp
import org.rfcx.audiomoth.entity.*

interface DeploymentProtocol : BaseDeploymentProtocol {
    fun openWithEdgeDevice()
    fun openWithGuardianDevice()

    fun startSetupConfigure(profile: Profile)
    fun startSyncing(status: String)
    fun startCheckBattery(status: String, level: Int?)

    fun getProfiles(): List<Profile>
    fun getProfile(): Profile?
    fun getDeployment(): EdgeDeployment?
    fun geConfiguration(): Configuration?

    fun setDeployment(deployment: EdgeDeployment)
    fun setProfile(profile: Profile)
    fun setDeploymentConfigure(profile: Profile)
    fun setPerformBattery(batteryDepletedAt: Timestamp, batteryLevel: Int)

    fun playSyncSound()
    fun playCheckBatterySound()
}
