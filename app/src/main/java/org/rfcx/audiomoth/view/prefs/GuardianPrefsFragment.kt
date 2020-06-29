package org.rfcx.audiomoth.view.prefs

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceFragmentCompat
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.OnReceiveResponse
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.entity.socket.PrefsResponse
import org.rfcx.audiomoth.entity.socket.SocketResposne

class GuardianPrefsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val LOGTAG = "GuardianPrefsFragment"

    private val prefsChanges = mutableMapOf<String, String>()

    private var onFirstTime = false

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs)
        setCurrentGuardianPrefs()
    }

    private fun setCurrentGuardianPrefs() {
        val prefsScreen = preferenceScreen
        val prefsEditor = prefsScreen.sharedPreferences.edit()
        SocketManager.getAllCurrentPrefs(object : OnReceiveResponse{
            override fun onReceive(response: SocketResposne) {
                val currentPrefs = response as PrefsResponse
                val listOfPrefs = currentPrefs.prefs.asJsonArray

                listOfPrefs.forEach {
                    val pref = it.asJsonObject
                    val key = ArrayList<String>(pref.keySet())[0]
                    val value = pref.get(key).asString.replace("\"", "")
                    prefsEditor.putString(key, value).apply()
                }
            }

            override fun onFailed(message: String) {
            }
        })
    }

    private fun sendChangedPrefs() {
        if(prefsChanges.isNotEmpty()) {
            val listForGuardian = mutableListOf<String>()
            prefsChanges.forEach {
                listForGuardian.add("${it.key}|${it.value}")
            }

            Log.d(LOGTAG, "Syncing Prefs: $listForGuardian")

            SocketManager.syncConfiguration(listForGuardian, object: OnReceiveResponse{
                override fun onReceive(response: SocketResposne) {
                    //TODO: show message(Toast or Snackbar) on DiagnosticPage
                }

                override fun onFailed(message: String) {
                    //TODO: show message(Toast or Snackbar) on DiagnosticPage
                }
            })
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val value = sharedPreferences!!.getString(key, "")
        prefsChanges[key!!] = value!!
        Log.d(LOGTAG, "Prefs changed: $key | $value")

        //update data in prefs
        if (!onFirstTime) {
            preferenceScreen = null
            addPreferencesFromResource(R.xml.prefs)
            onFirstTime = true
        }
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDetach() {
        super.onDetach()
        sendChangedPrefs()
    }

    override fun onDestroy() {
        super.onDestroy()
        //clear all guardian prefs when exit
        Log.d(LOGTAG, "Cleared all guardian prefs")
        preferenceScreen.sharedPreferences.edit().clear().apply()
    }

}
