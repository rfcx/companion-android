package org.rfcx.companion.util.prefs

import android.content.Context
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.google.gson.JsonParser

object PrefsUtils {

    fun stringToPrefs(context: Context, str: String): List<Preference> {
        val prefs = arrayListOf<Preference>()
        val json = JsonParser.parseString(str).asJsonObject
        val keys = json.keySet()
        keys.forEach {
            val pref = EditTextPreference(context)
            pref.key = it
            pref.text = json.get(it).asString
            pref.title = it
            pref.setDefaultValue(json.get(it).asString)
            prefs.add(pref)
        }
        return prefs
    }
}
