package org.rfcx.companion.util.prefs

import android.content.Context
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object PrefsUtils {

    const val audioDuration = "audio_cycle_duration"
    const val audioSampleRate = "audio_stream_sample_rate"
    const val audioCodec = "audio_stream_codec"
    const val audioBitrate = "audio_stream_bitrate"

    fun stringToPrefs(context: Context, str: String?): List<Preference> {
        if (str == null) {
            return listOf()
        }
        val prefs = arrayListOf<Preference>()
        val json = JsonParser.parseString(str).asJsonObject
        val keys = json.keySet()
        keys.sorted().forEach {
            val pref = EditTextPreference(context)
            pref.key = it
            pref.text = json.get(it).asString
            pref.title = it
            pref.setDefaultValue(json.get(it).asString)
            prefs.add(pref)
        }
        return prefs
    }

    fun getPurposeGuardiansFromPrefs(str: String?): String? {
        if (str == null) {
            return null
        }
        val json = JsonParser.parseString(str).asJsonObject
        val protocol = json.get("api_satellite_protocol").asString
        if (protocol == "off") return "cell"
        return protocol
    }

    fun getGuardianPlanFromPrefs(str: String?): GuardianPlan? {
        if (str == null) return null
        val json = JsonParser.parseString(str).asJsonObject
        val order = json.get("api_protocol_escalation_order").asString
        return when(order) {
            "mqtt,rest" -> GuardianPlan.CELL_ONLY
            "mqtt,rest,sms" -> GuardianPlan.CELL_SMS
            "sat" -> GuardianPlan.SAT_ONLY
            else -> null
        }
    }

    fun stringToAudioPrefs(str: String?): JsonObject? {
        if (str == null) {
            return null
        }
        val json = JsonParser.parseString(str).asJsonObject
        val keys = json.keySet()
        val audioPrefs = listOf(audioDuration, audioSampleRate, audioCodec, audioBitrate)
        val audioKeys = keys.filter { audioPrefs.contains(it) }
        val audioJson = JsonObject()
        audioKeys.toList().forEach {
            audioJson.add(it, json[it])
        }
        return audioJson
    }
}

enum class GuardianPlan{
    CELL_ONLY, CELL_SMS, SAT_ONLY
}
