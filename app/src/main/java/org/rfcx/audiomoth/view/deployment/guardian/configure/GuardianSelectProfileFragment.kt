package org.rfcx.audiomoth.view.deployment.guardian.configure

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_select_profile.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.entity.Screen
import org.rfcx.audiomoth.entity.guardian.GuardianProfile
import org.rfcx.audiomoth.util.Analytics
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianSelectProfileFragment : Fragment(), (GuardianProfile) -> Unit {
    private val profilesAdapter by lazy { GuardianProfilesAdapter(this) }
    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    private var profiles = listOf<GuardianProfile>()
    private var currentProfile: List<GuardianProfile>? = null
    private val analytics by lazy { context?.let { Analytics(it) } }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guardian_select_profile, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as GuardianDeploymentProtocol
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        getCurrentConfiguration()
    }

    // @{ProfilesAdapter.itemClickListener}
    override fun invoke(profile: GuardianProfile) {
        deploymentProtocol?.startSetupConfigure(profile)
    }

    private fun setupView() {
        deploymentProtocol?.hideCompleteButton()

        createNewButton.setOnClickListener {
            deploymentProtocol?.startSetupConfigure(GuardianProfile.default()) // new profile
        }

        profileRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = profilesAdapter
        }

        tryAgainTextView.setOnClickListener {
            getCurrentConfiguration()
        }
    }

    private fun getCurrentConfiguration() {
        checkState(SHOW_LOADING)

        SocketManager.getCurrentConfiguration()
        SocketManager.currentConfiguration.observe(viewLifecycleOwner, Observer { configuration ->
            currentProfile = listOf(
                GuardianProfile(
                    name = "Current configuration",
                    sampleRate = configuration.configure.sampleRate,
                    bitrate = configuration.configure.bitrate,
                    fileFormat = configuration.configure.fileFormat,
                    duration = configuration.configure.duration
                )
            )

            requireActivity().runOnUiThread {
                retrieveProfiles()
            }
        })
    }

    private fun retrieveProfiles() {
        val guardianProfiles = deploymentProtocol?.getProfiles()
        if (guardianProfiles!!.isNotEmpty()) {
            this.profiles = guardianProfiles
        } else {
            this.profiles = arrayListOf()
        }

        if (currentProfile != null) {
            profilesAdapter.items = currentProfile!! + profiles
        } else {
            profilesAdapter.items = profiles
        }
        checkState(SHOW_LIST_PROFILE)
    }

    private fun checkState(state: String) {
        when (state) {
            SHOW_LOADING -> {
                tryAgainTextView.visibility = View.GONE
                profileRecyclerView.visibility = View.GONE
                profileProgressBar.visibility = View.VISIBLE
            }
            SHOW_TRY_AGAIN -> {
                tryAgainTextView.visibility = View.VISIBLE
                profileRecyclerView.visibility = View.GONE
                profileProgressBar.visibility = View.GONE
            }
            SHOW_LIST_PROFILE -> {
                tryAgainTextView.visibility = View.GONE
                profileRecyclerView.visibility = View.VISIBLE
                profileProgressBar.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.GUARDIAN_SELECT_PROFILE)
    }

    companion object {
        const val SHOW_LOADING = "SHOW_LOADING"
        const val SHOW_TRY_AGAIN = "SHOW_TRY_AGAIN"
        const val SHOW_LIST_PROFILE = "SHOW_LIST_PROFILE"

        fun newInstance(): GuardianSelectProfileFragment {
            return GuardianSelectProfileFragment()
        }
    }
}
