package org.rfcx.companion.view.prefs

import android.content.SharedPreferences

interface SyncPreferenceListener {
    fun setPrefsChanges(prefs: Map<String, String>)
    fun getPrefsChanges(): List<String>
    fun showSyncButton()
    fun hideSyncButton()
    fun syncPrefs()
    fun showSuccessResponse()
    fun showFailedResponse()
    fun setEditor(editor: SharedPreferences.Editor)
}
