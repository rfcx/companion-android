package org.rfcx.companion.view.deployment.guardian

import com.google.gson.JsonObject
import org.rfcx.companion.connection.wifi.WifiHotspotManager
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.response.GuardianRegisterResponse
import org.rfcx.companion.entity.socket.response.I2CAccessibility
import org.rfcx.companion.entity.socket.response.SentinelInfo
import org.rfcx.companion.entity.socket.response.SpeedTest
import org.rfcx.companion.util.prefs.GuardianPlan
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol

interface GuardianDeploymentProtocol : BaseDeploymentProtocol {
    fun startSetupConfigure()
    fun startGuardianRegister()
    fun backToConfigure()

    fun getDeployment(): Deployment?
    fun getSampleRate(): Int
    fun getLastCheckInTime(): Long?
    fun getGuid(): String?
    fun getGuardianPurpose(): String?
    fun isGuardianRegistered(): Boolean?
    fun getSoftwareVersion(): Map<String, String>?
    fun getAudioConfiguration(): JsonObject?
    fun getPrefsSha1(): String?
    fun getLatestCheckIn(): JsonObject?
    fun reTriggerConnection()

    fun getNetwork(): Int?
    fun getSwmNetwork(): Int?
    fun getSwmUnsentMessages(): Int?
    fun getSentinelPower(): SentinelInfo?
    fun getInternalBattery(): Int?
    fun getI2cAccessibility(): I2CAccessibility?
    fun getSimDetected(): Boolean?
    fun getSatId(): String?
    fun getPhoneNumber(): String?
    fun getGuardianPlan(): GuardianPlan?
    fun getSatTimeOff(): List<String>?
    fun getSpeedTest(): SpeedTest?
    fun getGuardianLocalTime(): Long?
    fun getGuardianTimezone(): String?

    fun getCurrentProjectId(): String?

    fun setDeployment(deployment: Deployment)
    fun setSampleRate(sampleRate: Int)
    fun setOnDeployClicked()
    fun setLastCheckInTime(time: Long?)
    fun setGuardianRegisterBody(body: GuardianRegisterResponse?)

    fun addRegisteredToPassedCheck()
    fun removeRegisteredOnPassedCheck()

    fun showConnectInstruction()

    fun showLoading()
    fun hideLoading()
}
