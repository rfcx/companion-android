package org.rfcx.companion.util.socket

import android.content.Context
import android.util.Log
import androidx.preference.Preference
import com.google.gson.Gson
import org.rfcx.companion.entity.socket.response.AdminPing
import org.rfcx.companion.entity.socket.response.GuardianPing
import org.rfcx.companion.util.prefs.PrefsUtils

object PingUtils {

    fun getPrefsFromPing(context: Context, guardianPing: GuardianPing?): List<Preference> {
        val prefs = guardianPing?.prefs?.get("vals") ?: return listOf()
        Log.d("ConvertPing", prefs.toString())
        return PrefsUtils.stringToPrefs(context, Gson().toJson(prefs))
    }

    fun getPrefsSha1FromPing(guardianPing: GuardianPing?): String? {
        val sha1 = guardianPing?.prefs?.get("sha1") ?: return null
        Log.d("ConvertPing", sha1.toString())
        return sha1.asString
    }

    fun getNetworkFromPing(adminPing: AdminPing?): Int? {
        val network = adminPing?.network?.asInt ?: return null
        Log.d("ConvertPing", network.toString())
        return network
    }

    fun getSentinelPowerFromPing(adminPing: AdminPing?): String? {
        val sentinelPower = adminPing?.sentinelPower ?: return null
        val power = sentinelPower.get("power")
        val input = sentinelPower.get("input")
        val batt = sentinelPower.get("batt")
        return power.asString
    }
}
