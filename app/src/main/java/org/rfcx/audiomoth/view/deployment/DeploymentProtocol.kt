package org.rfcx.audiomoth.view.deployment

import java.sql.Timestamp
import org.rfcx.audiomoth.entity.*

interface DeploymentProtocol : BaseDeploymentProtocal {
    fun openWithEdgeDevice()
    fun openWithGuardianDevice()

    fun startSetupConfigure(profile: Profile)
    fun startSyncing(status: String)
    fun startCheckBattery(status: String, level: Int?)
    fun startMapPicker(latitude: Double, longitude: Double)
    fun startLocation(latitude: Double, longitude: Double)

    fun getProfiles(): List<Profile>
    fun getProfile(): Profile?
    fun getDeployment(): Deployment?
    fun geConfiguration(): Configuration?

    fun setDeployment(deployment: Deployment)
    fun setProfile(profile: Profile)
    fun setDeploymentConfigure(profile: Profile)
    fun setPerformBattery(batteryDepletedAt: Timestamp, batteryLevel: Int)

    fun playSyncSound()
    fun playCheckBatterySound()
}
