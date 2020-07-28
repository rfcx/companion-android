package org.rfcx.audiomoth.view.prefs

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceFragmentCompat
import org.rfcx.audiomoth.R

class GuardianPrefsFragment() : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val LOGTAG = "GuardianPrefsFragment"

    private val prefsChanges = mutableMapOf<String, String>()

    private var syncPreferenceListener: SyncPreferenceListener? = null

    private var switchPrefs = listOf(
        "show_ui",
        "enable_audio_capture",
        "enable_checkin_publish",
        "enable_cutoffs_battery",
        "enable_cutoffs_schedule_off_hours",
        "admin_enable_log_capture",
        "admin_enable_screenshot_capture",
        "admin_enable_bluetooth",
        "admin_enable_wifi",
        "admin_enable_tcp_adb",
        "admin_enable_sentinel_capture",
        "admin_enable_ssh_server",
        "admin_enable_wifi_socket"
    )

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        syncPreferenceListener = context as SyncPreferenceListener
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs)
        syncPreferenceListener?.setEditor(preferenceScreen.sharedPreferences.edit())
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val value = if (switchPrefs.contains(key)) {
            sharedPreferences?.getBoolean(key, false).toString()
        } else {
            sharedPreferences?.getString(key, "") ?: ""
        }

        prefsChanges[key!!] = value
        Log.d(LOGTAG, "Prefs changed: $key | $value")

        syncPreferenceListener?.showSyncButton()
        syncPreferenceListener?.setPrefsChanges(prefsChanges)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}
