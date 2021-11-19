package org.rfcx.companion.util.socket

import android.content.Context
import androidx.preference.Preference
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.rfcx.companion.entity.socket.response.AdminPing
import org.rfcx.companion.entity.socket.response.GuardianPing
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

    fun getSwarmNetworkFromPing(adminPing: AdminPing?): Int? {
        val network = adminPing?.swmNetwork ?: return null
        return network.split("*")[1].toInt()
    }

    fun getSentinelPowerFromPing(adminPing: AdminPing?): String? {
        val sentinelPower = adminPing?.sentinelPower ?: return null
        val power = sentinelPower
        val input = sentinelPower
        val batt = sentinelPower
        return power
    }

    fun getGuidFromPing(ping: GuardianPing?): String? {
        val guid = ping?.companion?.get("guardian")?.asJsonObject?.get("guid") ?: return null
        return guid.asString
    }

    fun isRegisteredFromPing(ping: GuardianPing?): Boolean? {
        val isRegistered = ping?.companion?.get("is_registered") ?: return null
        return isRegistered.asBoolean
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
}
