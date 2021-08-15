package org.rfcx.companion.util.socket

import android.content.Context
import android.util.Log
import androidx.preference.Preference
import com.google.gson.Gson
import org.rfcx.companion.entity.socket.response.Ping
import org.rfcx.companion.util.prefs.PrefsUtils

object PingUtils {

    fun getPrefsFromPing(context: Context, ping: Ping?): List<Preference> {
        val prefs = ping?.prefs?.get("vals") ?: return listOf()
        Log.d("ConvertPing", prefs.toString())
        return PrefsUtils.stringToPrefs(context, Gson().toJson(prefs))
    }

    fun getPrefsSha1FromPing(ping: Ping?): String? {
        val sha1 = ping?.prefs?.get("sha1") ?: return null
        Log.d("ConvertPing", sha1.toString())
        return sha1.asString
    }

    fun getGuidFromPing(ping: Ping?): String? {
        val guid = ping?.companion?.get("guid") ?: return null
        return guid.asString
    }

    fun isRegisteredFromPing(ping: Ping?): Boolean? {
        val isRegistered = ping?.companion?.get("is_registered") ?: return null
        Log.d("ConvertPing", isRegistered.asBoolean.toString())
        return isRegistered.asBoolean
    }
}
