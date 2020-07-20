package org.rfcx.audiomoth.view.deployment

import org.rfcx.audiomoth.entity.*
import java.sql.Timestamp

interface DeploymentProtocol : BaseDeploymentProtocal {
    fun openWithEdgeDevice()
    fun openWithGuardianDevice()

    fun startSetupConfigure(profile: Profile)
    fun startSyncing(status: String)
    fun startCheckBattery(status: String, level: Int?)
    fun startMapPicker()
    fun startLocation(latitude: Double, longitude: Double)

    fun getProfiles(): List<Profile>
    fun getProfile(): Profile?
    fun getDeployment(): Deployment?
    fun geConfiguration(): Configuration?
    fun getDeploymentImage(): List<DeploymentImage>

    fun setDeployment(deployment: Deployment)
    fun setProfile(profile: Profile)
    fun setDeploymentConfigure(profile: Profile)
    fun setPerformBattery(batteryDepletedAt: Timestamp, batteryLevel: Int)

    fun playSyncSound()
    fun playCheckBatterySound()
}
