package org.rfcx.companion.view.prefs

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.rfcx.companion.R

class GuardianPrefsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefsChanges = mutableMapOf<String, String>()

    private var syncPreferenceListener: SyncPreferenceListener? = null

    private var prefs: List<Preference> = listOf()

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        syncPreferenceListener = context as SyncPreferenceListener
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        prefs.forEach {
            preferenceScreen.addPreference(it)
        }
        syncPreferenceListener?.setEditor(preferenceScreen.sharedPreferences.edit())
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val value = sharedPreferences?.getString(key, "") ?: ""

        prefsChanges[key!!] = value

        syncPreferenceListener?.showSyncButton()
        syncPreferenceListener?.setPrefsChanges(prefsChanges)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}
