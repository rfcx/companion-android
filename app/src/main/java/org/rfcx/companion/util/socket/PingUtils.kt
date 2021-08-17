package org.rfcx.companion.util.socket

import android.content.Context
import android.util.Log
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
            Log.d("ConvertPing", prefs.toString())
            return PrefsUtils.stringToPrefs(context, Gson().toJson(prefs))
        }
        return listOf()
    }

    fun getPrefsSha1FromPing(guardianPing: GuardianPing?): String? {
        if (guardianPing?.prefs is JsonObject) {
            val sha1 = guardianPing.prefs.get("sha1") ?: return null
            Log.d("ConvertPing", sha1.toString())
            return sha1.asString
        }
        return null
    }

    fun getNetworkFromPing(adminPing: AdminPing?): Int? {
        val network = adminPing?.network ?: return null
        return network.split("*")[1].toInt()
    }

    fun getSentinelPowerFromPing(adminPing: AdminPing?): String? {
        val sentinelPower = adminPing?.sentinelPower ?: return null
        val power = sentinelPower
        val input = sentinelPower
        val batt = sentinelPower
        return power
    }
}
