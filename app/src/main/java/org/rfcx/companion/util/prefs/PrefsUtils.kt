package org.rfcx.companion.util.prefs

import android.content.Context
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object PrefsUtils {

    const val audioDuration = "audio_cycle_duration"
    const val audioSampleRate = "audio_stream_sample_rate"
    const val audioCodec = "audio_stream_codec"
    const val audioBitrate = "audio_stream_bitrate"
    const val audioCastSampleRate = "audio_cast_sample_rate_minimum"
    const val enableSampling = "enable_cutoffs_sampling_ratio"
    const val sampling = "audio_sampling_ratio"

    fun stringToPrefs(context: Context, str: String?): List<Preference> {
        if (str == null) {
            return listOf()
        }
        val prefs = arrayListOf<Preference>()
        val json = JsonParser.parseString(str).asJsonObject
        val keys = json.keySet()
        keys.sorted().forEach {
            var pref: Preference = EditTextPreference(context)
            if (json.get(it).asString == "true" || json.get(it).asString == "false") {
                pref = ListPreference(context)
                pref.entryValues = arrayOf("true", "false")
                pref.entries = arrayOf("true", "false")
            }
            if (it == "api_satellite_protocol") {
                pref = ListPreference(context)
                pref.entryValues = arrayOf("off", "sbd", "swm")
                pref.entries = arrayOf("off", "sbd", "swm")
            }

            if (pref is EditTextPreference) {
                pref.text = json.get(it).asString
            }
            pref.key = it
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

    fun getSampleRateFromPrefs(str: String?): Int? {
        if (str == null) return null
        val json = JsonParser.parseString(str).asJsonObject
        return json.get("audio_stream_sample_rate").asInt
    }

    fun getGuardianPlanFromPrefs(str: String?): GuardianPlan? {
        if (str == null) return null
        val json = JsonParser.parseString(str).asJsonObject
        val order = json.get("api_protocol_escalation_order").asString
        return when (order) {
            "mqtt,rest" -> GuardianPlan.CELL_ONLY
            "mqtt,rest,sms" -> GuardianPlan.CELL_SMS
            "sat" -> GuardianPlan.SAT_ONLY
            else -> null
        }
    }

    fun getSatTimeOffFromPrefs(str: String?): List<String>? {
        if (str == null) return null
        val json = JsonParser.parseString(str).asJsonObject
        val timeOff = json.get("api_satellite_off_hours").asString
        return timeOff.split(",")
    }

    fun stringToAudioPrefs(str: String?): JsonObject? {
        if (str == null) {
            return null
        }
        val json = JsonParser.parseString(str).asJsonObject
        val keys = json.keySet()
        val audioPrefs = listOf(audioDuration, audioSampleRate, audioCodec, audioBitrate, enableSampling, sampling)
        val audioKeys = keys.filter { audioPrefs.contains(it) }
        val audioJson = JsonObject()
        audioKeys.toList().forEach {
            audioJson.add(it, json[it])
        }
        return audioJson
    }
}

enum class GuardianPlan {
    CELL_ONLY, CELL_SMS, SAT_ONLY
}
