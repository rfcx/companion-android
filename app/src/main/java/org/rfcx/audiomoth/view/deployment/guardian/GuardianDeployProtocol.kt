package org.rfcx.audiomoth.view.deployment.guardian

interface GuardianDeployProtocol {
    fun setCompleteTextButton(text: String)
    fun hideCompleteButton()
    fun showCompleteButton()
    fun nextStep()
    fun backStep()

//    fun startSetupConfigure(profile: Profile)
    fun startSetupConfigure()
    fun startSyncing(status: String)
    fun startCheckBattery(status: String, level: Int?)

//    fun getProfiles(): List<Profile>
//    fun getProfile(): Profile?
//    fun getDeployment(): Deployment?
//    fun geConfiguration(): Configuration?
//    fun getDeploymentLocation(): DeploymentLocation?

//    fun setDeployment(deployment: Deployment)
//    fun setDeployLocation(locate: Locate)
//    fun setProfile(profile: Profile)
//    fun setDeploymentConfigure(profile: Profile)
//    fun setPerformBattery(batteryDepletedAt: Timestamp, batteryLevel: Int)

    fun setReadyToDeploy(images: List<String>)
}