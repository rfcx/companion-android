package org.rfcx.audiomoth.view.deployment.guardian.configure


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_guardian_select_profile.*
import kotlinx.android.synthetic.main.fragment_select_profile.createNewButton
import kotlinx.android.synthetic.main.fragment_select_profile.profileProgressBar
import kotlinx.android.synthetic.main.fragment_select_profile.profileRecyclerView
import kotlinx.android.synthetic.main.fragment_select_profile.tryAgainTextView
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.OnReceiveResponse
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.entity.guardian.GuardianProfile
import org.rfcx.audiomoth.entity.socket.ConfigurationResponse
import org.rfcx.audiomoth.entity.socket.SocketResposne
import org.rfcx.audiomoth.view.deployment.configure.ProfilesAdapter
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianSelectProfileFragment : Fragment(), (GuardianProfile) -> Unit {
    private val profilesAdapter by lazy { GuardianProfilesAdapter(this) }
    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    private var profiles = listOf<GuardianProfile>()
    private var currentProfile: GuardianProfile? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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

        defaultProfileLayout.setOnClickListener {
            deploymentProtocol?.startSetupConfigure(currentProfile!!)
        }

        tryAgainTextView.setOnClickListener {
            getCurrentConfiguration()
        }
    }

    private fun getCurrentConfiguration() {
        checkState(SHOW_LOADING)
        SocketManager.getCurrentConfiguration(object : OnReceiveResponse {
            override fun onReceive(response: SocketResposne) {
                val config = response as ConfigurationResponse
                setCurrentConfiguration(config)
                retrieveProfiles()

                currentProfile = GuardianProfile(
                    sampleRate = config.sampleRate,
                    bitrate = config.bitrate,
                    fileFormat = config.fileFormat,
                    duration = config.duration
                )
            }

            override fun onFailed() {
                checkState(SHOW_TRY_AGAIN)
            }
        })
    }

    private fun setCurrentConfiguration(config: ConfigurationResponse) {
        defaultDetailTextView.text = context!!.getString(
            R.string.configuration_details,
            config.fileFormat,
            config.sampleRate,
            config.bitrate,
            config.duration
        )
    }

    private fun retrieveProfiles() {
        this.profiles = deploymentProtocol?.getProfiles() ?: arrayListOf()
        profilesAdapter.items = profiles
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

    companion object {
        const val SHOW_LOADING = "SHOW_LOADING"
        const val SHOW_TRY_AGAIN = "SHOW_TRY_AGAIN"
        const val SHOW_LIST_PROFILE = "SHOW_LIST_PROFILE"

        fun newInstance(): GuardianSelectProfileFragment {
            return GuardianSelectProfileFragment()
        }
    }
}
