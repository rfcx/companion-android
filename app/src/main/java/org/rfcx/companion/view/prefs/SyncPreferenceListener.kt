package org.rfcx.companion.view.prefs

import android.content.SharedPreferences
import androidx.preference.Preference

interface SyncPreferenceListener {
    fun setPrefsChanges(prefs: Map<String, String>)
    fun getPrefsChanges(): String
    fun showSyncButton()
    fun hideSyncButton()
    fun syncPrefs()
    fun showSuccessResponse()
    fun showFailedResponse()
    fun setEditor(editor: SharedPreferences.Editor)

    fun getPrefs(): List<Preference>?
}
