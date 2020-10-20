package org.rfcx.companion.view.prefs

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.rfcx.companion.R

class GuardianPrefsFragment() : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefsChanges = mutableMapOf<String, String>()

    private var syncPreferenceListener: SyncPreferenceListener? = null

    private var switchPrefs: List<String> = listOf()

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        syncPreferenceListener = context as SyncPreferenceListener
        switchPrefs = context.resources.getStringArray(R.array.switch_prefs).toList()
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

        syncPreferenceListener?.showSyncButton()
        syncPreferenceListener?.setPrefsChanges(prefsChanges)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}
