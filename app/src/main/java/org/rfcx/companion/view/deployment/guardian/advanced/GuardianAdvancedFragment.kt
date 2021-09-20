package org.rfcx.companion.view.deployment.guardian.advanced

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.fragment_guardian_advanced.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.GuardianSocketManager
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol
import org.rfcx.companion.view.prefs.GuardianPrefsFragment
import org.rfcx.companion.view.prefs.SyncPreferenceListener

class GuardianAdvancedFragment : Fragment() {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    private var syncPreferenceListener: SyncPreferenceListener? = null

    private var needCheckSha1 = false
    private var currentPrefsSha1: String? = null

    private val analytics by lazy { context?.let { Analytics(it) } }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as GuardianDeploymentProtocol
        syncPreferenceListener = context as SyncPreferenceListener
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
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
        }

        //start guardian prefs fragment once view created
        parentFragmentManager.beginTransaction()
            .replace(advancedContainer.id, GuardianPrefsFragment())
            .commit()

        advancedFinishButton.setOnClickListener {
            analytics?.trackClickNextEvent(Screen.GUARDIAN_ADVANCED.id)
            syncConfig()
        }
    }

    private fun syncConfig() {
        val prefs = syncPreferenceListener?.getPrefsChanges() ?: JsonObject()
        needCheckSha1 = prefs.size() > 0
        GuardianSocketManager.syncConfiguration(prefs.toString())
        GuardianSocketManager.syncConfiguration.observe(viewLifecycleOwner, Observer {
            requireActivity().runOnUiThread {
                if (!needCheckSha1) {
                    deploymentProtocol?.nextStep()
                }
                if (currentPrefsSha1 != deploymentProtocol?.getPrefsSha1()) {
                    deploymentProtocol?.nextStep()
                }
            }
        })
    }

    override fun onDetach() {
        super.onDetach()
        GuardianSocketManager.resetPrefsValue()
    }

    companion object {
        fun newInstance(): GuardianAdvancedFragment {
            return GuardianAdvancedFragment()
        }
    }
}
