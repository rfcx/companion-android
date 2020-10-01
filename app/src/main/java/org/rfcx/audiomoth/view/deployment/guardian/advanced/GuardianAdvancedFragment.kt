package org.rfcx.audiomoth.view.deployment.guardian.advanced

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.google.gson.JsonArray
import kotlinx.android.synthetic.main.fragment_guardian_advanced.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.entity.socket.response.Status
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol
import org.rfcx.audiomoth.view.prefs.GuardianPrefsFragment
import org.rfcx.audiomoth.view.prefs.SyncPreferenceListener

class GuardianAdvancedFragment : Fragment() {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    private var syncPreferenceListener: SyncPreferenceListener? = null

    private var switchPrefs: List<String>? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as GuardianDeploymentProtocol
        syncPreferenceListener = context as SyncPreferenceListener
        setPredefinedConfiguration(context)
    }

    private fun setPredefinedConfiguration(context: Context) {
        switchPrefs = context.resources.getStringArray(R.array.switch_prefs).toList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guardian_advanced, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.showToolbar()
            it.setToolbarTitle()
        }

        //start guardian prefs fragment once view created
        parentFragmentManager.beginTransaction()
            .replace(advancedContainer.id, GuardianPrefsFragment())
            .commit()
        retrieveAllPrefs()

        advancedFinishButton.setOnClickListener {
            syncConfig()
        }
    }

    private fun retrieveAllPrefs() {
        SocketManager.getAllPrefs()
        SocketManager.prefs.observe(viewLifecycleOwner, Observer {
            setupCurrentPrefs(it.prefs)
        })
    }

    private fun setupCurrentPrefs(prefs: JsonArray) {
        val prefsEditor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
        prefs.forEach {
            val pref = it.asJsonObject
            val key = ArrayList<String>(pref.keySet())[0]
            val value = pref.get(key).asString.replace("\"", "")
            if (switchPrefs!!.contains(key)) {
                prefsEditor.putBoolean(key, value.toBoolean()).apply()
            } else {
                prefsEditor.putString(key, value).apply()
            }
        }
    }

    private fun syncConfig() {
        val prefs = syncPreferenceListener?.getPrefsChanges() ?: listOf()
        SocketManager.syncConfiguration(prefs)
        SocketManager.syncConfiguration.observe(viewLifecycleOwner, Observer {
            requireActivity().runOnUiThread {
                if (it.sync.status == Status.SUCCESS.value) {
                    deploymentProtocol?.nextStep()
                }
            }
        })
    }

    override fun onDetach() {
        super.onDetach()
        SocketManager.resetPrefsValue()
    }

    companion object {
        fun newInstance(): GuardianAdvancedFragment {
            return GuardianAdvancedFragment()
        }
    }
}