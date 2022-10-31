package org.rfcx.companion.view.deployment.guardian

import com.google.gson.JsonObject
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.guardian.ClassifierLite
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.socket.response.*
import org.rfcx.companion.util.prefs.GuardianPlan
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol

interface GuardianDeploymentProtocol : BaseDeploymentProtocol {
    fun startConnectGuardian()
    fun startSetupConfigure()
    fun startGuardianRegister()
    fun backToConfigure()

    fun registerWifiLost()
    fun unregisterWifiLost()

    fun getDeployment(): Deployment?
    fun getSampleRate(): Int
    fun getLastCheckInTime(): Long?
    fun getGuid(): String?
    fun getGuardianPurpose(): String?
    fun isGuardianRegistered(): Boolean?
    fun isSMSOrSatGuardian(): Boolean
    fun getSoftwareVersion(): Map<String, String>?
    fun getAudioConfiguration(): JsonObject?
    fun getPrefsSha1(): String?
    fun getLatestCheckIn(): JsonObject?
    fun reTriggerConnection()
    fun startPeriodicHeartbeat()
    fun stopPeriodicHeartbeat()

    fun getNetwork(): Int?
    fun getSwmNetwork(): Int?
    fun getSwmUnsentMessages(): Int?
    fun getSentinelPower(): SentinelInfo?
    fun getInternalBattery(): Int?
    fun getI2cAccessibility(): I2CAccessibility?
    fun getSimDetected(): Boolean?
    fun getSatId(): String?
    fun getGPSDetected(): Boolean?
    fun getPhoneNumber(): String?
    fun getGuardianPlan(): GuardianPlan?
    fun getSatTimeOff(): List<String>?
    fun getSpeedTest(): SpeedTest?
    fun getGuardianLocalTime(): Long?
    fun getGuardianTimezone(): String?
    fun getClassifiers(): Map<String, ClassifierLite>?
    fun getActiveClassifiers(): Map<String, ClassifierLite>?
    fun getAudioCapturing(): AudioCaptureStatus?
    fun getStorage(): GuardianStorage?

    fun getCurrentProjectId(): String?
    fun getCurrentProject(): Project?

    fun setDeployment(deployment: Deployment)
    fun setSampleRate(sampleRate: Int)
    fun setOnDeployClicked()
    fun setLastCheckInTime(time: Long?)

    fun addRegisteredToPassedCheck()
    fun removeRegisteredOnPassedCheck()

    fun showConnectInstruction()

    fun showLoading()
    fun hideLoading()
}
