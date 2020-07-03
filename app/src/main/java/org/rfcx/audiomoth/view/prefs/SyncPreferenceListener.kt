package org.rfcx.audiomoth.view.prefs

interface SyncPreferenceListener {
    fun setPrefsChanges(prefs: Map<String, String>)
    fun showSyncButton()
    fun hideSyncButton()
    fun syncPrefs(prefs: Map<String, String>)
    fun showSuccessResponse()
    fun showFailedResponse()
}
