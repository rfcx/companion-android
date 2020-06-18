package org.rfcx.audiomoth.view.deployment

import org.rfcx.audiomoth.entity.*
import java.sql.Timestamp

interface DeploymentProtocol {
    fun openWithEdgeDevice()
    fun openWithGuardianDevice()
    fun setCompleteTextButton(text: String)
    fun hideCompleteButton()
    fun showCompleteButton()
    fun hideStepView()
    fun showStepView()
    fun nextStep()
    fun backStep()

    fun startSetupConfigure(profile: Profile)
    fun startSyncing(status: String)
    fun startCheckBattery(status: String, level: Int?)

    fun getProfiles(): List<Profile>
    fun getProfile(): Profile?
    fun getDeployment(): Deployment?
    fun geConfiguration(): Configuration?
    fun getDeploymentLocation(): DeploymentLocation?

    fun setDeployment(deployment: Deployment)
    fun setDeployLocation(locate: Locate)
    fun setProfile(profile: Profile)
    fun setDeploymentConfigure(profile: Profile)
    fun setPerformBattery(batteryDepletedAt: Timestamp, batteryLevel: Int)
    fun setReadyToDeploy(images: List<String>)

    fun playSyncSound()
    fun playCheckBatterySound()
}