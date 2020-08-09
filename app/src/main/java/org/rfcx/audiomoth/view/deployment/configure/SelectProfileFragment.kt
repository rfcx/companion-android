package org.rfcx.audiomoth.view.deployment.configure

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_select_profile.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Profile
import org.rfcx.audiomoth.view.deployment.EdgeDeploymentProtocol

class SelectProfileFragment : Fragment(), (Profile) -> Unit {
    private val profilesAdapter by lazy { ProfilesAdapter(this) }
    private var edgeDeploymentProtocol: EdgeDeploymentProtocol? = null
    private var profiles = listOf<Profile>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_profile, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        edgeDeploymentProtocol = context as EdgeDeploymentProtocol
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        retrieveProfiles()
    }

    // @{ProfilesAdapter.itemClickListener}
    override fun invoke(profile: Profile) {
        edgeDeploymentProtocol?.startSetupConfigure(profile)
    }

    private fun setupView() {
        edgeDeploymentProtocol?.hideCompleteButton()

        createNewButton.setOnClickListener {
            edgeDeploymentProtocol?.startSetupConfigure(Profile.default()) // new profile
        }

        profileRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = profilesAdapter
        }

        tryAgainTextView.setOnClickListener {
            retrieveProfiles()
        }
    }

    private fun retrieveProfiles() {
        checkState(SHOW_LOADING)
        this.profiles = edgeDeploymentProtocol?.getProfiles() ?: arrayListOf()
        if (profiles.isNotEmpty()) {
            profilesAdapter.items = profiles
            checkState(SHOW_LIST_PROFILE)
        } else {
            checkState(SHOW_TRY_AGAIN)
        }
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

        fun newInstance(): SelectProfileFragment {
            return SelectProfileFragment()
        }
    }
}
