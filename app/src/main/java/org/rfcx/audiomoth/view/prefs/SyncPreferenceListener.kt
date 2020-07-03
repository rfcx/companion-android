package org.rfcx.audiomoth.view.prefs

import android.content.SharedPreferences

interface SyncPreferenceListener {
    fun setPrefsChanges(prefs: Map<String, String>)
    fun showSyncButton()
    fun hideSyncButton()
    fun syncPrefs(prefs: Map<String, String>)
    fun showSuccessResponse()
    fun showFailedResponse()
    fun setEditor(editor: SharedPreferences.Editor)
}
