package org.rfcx.companion.view.prefs

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import org.rfcx.companion.R

class GuardianPrefsFragment :
    PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefsChanges = mutableMapOf<String, String>()

    private var syncPreferenceListener: SyncPreferenceListener? = null

    private var prefs: List<Preference> = listOf()

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        syncPreferenceListener = context as SyncPreferenceListener
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs)
        val preferenceCategory = PreferenceCategory(preferenceScreen.context)
        preferenceCategory.title = ""
        preferenceScreen.addPreference(preferenceCategory)

        prefs = syncPreferenceListener?.getPrefs() ?: listOf()
        prefs.forEach {
            preferenceCategory.addPreference(it)
        }
        syncPreferenceListener?.setEditor(preferenceScreen.sharedPreferences?.edit()!!)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val value = sharedPreferences?.getString(key, "")

        if (value != null && value != "") {
            prefsChanges[key!!] = value
        }

        syncPreferenceListener?.showSyncButton()
        syncPreferenceListener?.setPrefsChanges(prefsChanges)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }
}
