package org.rfcx.companion.util.socket

import android.content.Context
import androidx.preference.Preference
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.rfcx.companion.entity.guardian.ClassifierLite
import org.rfcx.companion.entity.socket.response.*
import org.rfcx.companion.util.prefs.GuardianPlan
import org.rfcx.companion.util.prefs.PrefsUtils

object PingUtils {

    fun getPrefsFromPing(context: Context, guardianPing: GuardianPing?): List<Preference> {
        if (guardianPing?.prefs is JsonObject) {
            val prefs = guardianPing.prefs.get("vals") ?: return listOf()
            return PrefsUtils.stringToPrefs(context, Gson().toJson(prefs))
        }
        return listOf()
    }

    fun getPrefsSha1FromPing(guardianPing: GuardianPing?): String? {
        if (guardianPing?.prefs is JsonObject) {
            val sha1 = guardianPing.prefs.get("sha1") ?: return null
            return sha1.asString
        }
        return null
    }

    fun getNetworkFromPing(adminPing: AdminPing?): Int? {
        val network = adminPing?.network ?: return null
        return network.split("*")[1].toInt()
    }

    fun getSwarmNetworkFromPing(guardianPing: GuardianPing?): Int? {
        val network = guardianPing?.swm ?: return null
        val splitNetworks = network.split("|").map { it.split("*") }
        return splitNetworks.last()[1].toIntOrNull()
    }

    fun getSwarmUnsetMessagesFromPing(guardianPing: GuardianPing?): Int? {
        val network = guardianPing?.swm ?: return null
        val splitNetworks = network.split("|").map { it.split("*") }
        val lastSwmObj = splitNetworks.last()
        return lastSwmObj[lastSwmObj.size - 1].toIntOrNull()
    }

    fun getI2cAccessibilityFromPing(adminPing: AdminPing?): I2CAccessibility? {
        val i2c = adminPing?.companion?.get("i2c")?.asJsonObject ?: return null
        return Gson().fromJson(i2c, I2CAccessibility::class.java)
    }

    fun getInternalBatteryFromPing(guardianPing: GuardianPing?): Int? {
        val battery = guardianPing?.battery ?: return null
        return battery.split("*")[1].toInt()
    }

    fun getSentinelPowerFromPing(adminPing: AdminPing?): SentinelInfo? {
        val sentinelPower = adminPing?.sentinelPower ?: return null
        val splitSentinelPower = sentinelPower.split("|")
        var system = SentinelSystem()
        var input = SentinelInput()
        var batt = SentinelBattery()
        try {
            splitSentinelPower.forEach {
                val splittedItem = it.split("*")
                when (splittedItem[0]) {
                    "system" -> system = SentinelSystem(
                        splittedItem[2].toInt(),
                        splittedItem[3].toInt(),
                        splittedItem[4].toInt(),
                        splittedItem[5].toInt()
                    )
                    "input" -> input = SentinelInput(
                        splittedItem[2].toInt(),
                        splittedItem[3].toInt(),
                        splittedItem[4].toInt(),
                        splittedItem[5].toInt()
                    )
                    "battery" -> batt = SentinelBattery(
                        splittedItem[2].toInt(),
                        splittedItem[3].toInt(),
                        splittedItem[4].toDouble(),
                        splittedItem[5].toInt()
                    )
                }
            }
        } catch (e: NumberFormatException) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
        return SentinelInfo(input, system, batt)
    }

    fun getGuidFromPing(ping: GuardianPing?): String? {
        val guid = ping?.companion?.get("guardian")?.asJsonObject?.get("guid") ?: return null
        return guid.asString
    }

    fun getPurposeFromPrefs(guardianPing: GuardianPing?): String? {
        if (guardianPing?.prefs is JsonObject) {
            val prefs = guardianPing.prefs.get("vals") ?: return null
            return PrefsUtils.getPurposeGuardiansFromPrefs(Gson().toJson(prefs))
        }
        return null
    }

    fun getGuardianPlanFromPrefs(guardianPing: GuardianPing?): GuardianPlan? {
        if (guardianPing?.prefs is JsonObject) {
            val prefs = guardianPing.prefs.get("vals") ?: return null
            return PrefsUtils.getGuardianPlanFromPrefs(Gson().toJson(prefs))
        }
        return null
    }

    fun getSampleRateFromPrefs(guardianPing: GuardianPing?): Int? {
        if (guardianPing?.prefs is JsonObject) {
            val prefs = guardianPing.prefs.get("vals") ?: return null
            return PrefsUtils.getSampleRateFromPrefs(Gson().toJson(prefs))
        }
        return null
    }

    fun getSatTimeOffFromPrefs(guardianPing: GuardianPing?): List<String>? {
        if (guardianPing?.prefs is JsonObject) {
            val prefs = guardianPing.prefs.get("vals") ?: return null
            return PrefsUtils.getSatTimeOffFromPrefs(Gson().toJson(prefs))
        }
        return null
    }

    fun isRegisteredFromPing(ping: GuardianPing?): Boolean? {
        val isRegistered = ping?.companion?.get("is_registered") ?: return null
        return isRegistered.asBoolean
    }

    fun isSMSOrSatGuardian(guardianPing: GuardianPing?): Boolean {
        if (guardianPing?.prefs is JsonObject) {
            val prefs = guardianPing.prefs.get("vals") ?: return false
            return PrefsUtils.isSMSOrSatGuardian(Gson().toJson(prefs))
        }
        return false
    }

    fun getSoftwareVersionFromPing(ping: GuardianPing?): Map<String, String>? {
        val software = ping?.software ?: return null
        val softwareList = software.split("|")
        val mapSoftwareVersion = mutableMapOf<String, String>()
        softwareList.forEach {
            val role = it.split("*")[0]
            val version = it.split("*")[1]
            mapSoftwareVersion[role] = version
        }
        return mapSoftwareVersion
    }

    fun getAudioConfigureFromPing(ping: GuardianPing?): JsonObject? {
        if (ping?.prefs is JsonObject) {
            val prefs = ping.prefs.get("vals") ?: return null
            return PrefsUtils.stringToAudioPrefs(Gson().toJson(prefs))
        }
        return null
    }

    fun getLatestCheckInFromPing(ping: GuardianPing?): JsonObject? {
        if (ping?.prefs is JsonObject) {
            val checkIn = ping.companion?.get("checkin") ?: return null
            return checkIn.asJsonObject
        }
        return null
    }

    fun getGuardianLocalTime(ping: GuardianPing?): Long? {
        val localtime = ping?.companion?.get("system_time_utc") ?: return null
        return localtime.asLong
    }

    fun getGuardianTimezone(ping: GuardianPing?): String? {
        val timezone = ping?.companion?.get("system_timezone") ?: return null
        return timezone.asString
    }

    fun getClassifiers(ping: GuardianPing?): Map<String, ClassifierLite>? {
        val library = ping?.library ?: return null
        library.let { lib ->
            if (lib.has("classifiers")) {
                val classifiers = lib.get("classifiers").asJsonArray
                if (classifiers.size() > 0) {
                    val map = mutableMapOf<String, ClassifierLite>()
                    classifiers.map { clsf ->
                        ClassifierLite(
                            id = clsf.asJsonObject.get("id").asString,
                            name = clsf.asJsonObject.get("guid").asString.split("-v")[0],
                            version = clsf.asJsonObject.get("guid").asString.split("-v")[1]
                        )
                    }.forEach { clsf ->
                        map[clsf.id] = clsf
                    }
                    return map
                }
                return null
            }
            return null
        }
    }

    fun getActiveClassifiers(ping: GuardianPing?): Map<String, ClassifierLite>? {
        val library = ping?.activeClassifier ?: return null
        library.let { lib ->
            val activeClassifiers = lib.asJsonArray
            if (activeClassifiers.size() > 0) {
                val map = mutableMapOf<String, ClassifierLite>()
                activeClassifiers.map { clsf ->
                    ClassifierLite(
                        id = clsf.asJsonObject.get("id").asString,
                        name = clsf.asJsonObject.get("guid").asString.split("-v")[0],
                        version = clsf.asJsonObject.get("guid").asString.split("-v")[1]
                    )
                }.forEach { clsf ->
                    map[clsf.id] = clsf
                }
                return map
            }
            return null
        }
    }

    fun getAudioCaptureStatus(ping: GuardianPing?): AudioCaptureStatus? {
        val isCapturing = ping?.companion?.get("is_audio_capturing") ?: return null
        val captureMsg = ping.companion.get("audio_capturing_message") ?: null
        return AudioCaptureStatus(isCapturing.asBoolean, captureMsg?.asString)
    }

    fun getSpeedTest(ping: AdminPing?): SpeedTest? {
        val speedTest = ping?.companion?.get("speed_test")?.asJsonObject ?: return null
        val downloadSpeed = speedTest.get("download_speed").asDouble
        val uploadSpeed = speedTest.get("upload_speed").asDouble
        val isFailed = speedTest.get("is_failed").asBoolean
        val isTesting =
            if (speedTest.has("is_testing")) speedTest.get("is_testing").asBoolean else false
        val hasConnection = speedTest.get("connection_available").asBoolean
        return SpeedTest(downloadSpeed, uploadSpeed, isFailed, isTesting, hasConnection)
    }

    fun getSimDetectedFromPing(adminPing: AdminPing?): Boolean? {
        return adminPing?.companion?.get("sim_info")?.asJsonObject?.get("has_sim")?.asBoolean
            ?: return null
    }

    fun getGPSDetectedFromPing(adminPing: AdminPing?): Boolean? {
        return adminPing?.companion?.get("sat_info")?.asJsonObject?.get("is_gps_connected")?.asBoolean
            ?: return null
    }

    fun getPhoneNumberFromPing(adminPing: AdminPing?): String? {
        return adminPing?.companion?.get("sim_info")?.asJsonObject?.get("phone_number")?.asString
            ?: return null
    }

    fun getSwarmIdFromPing(adminPing: AdminPing?): String? {
        return adminPing?.companion?.get("sat_info")?.asJsonObject?.get("sat_id")?.asString
            ?: return null
    }
}
