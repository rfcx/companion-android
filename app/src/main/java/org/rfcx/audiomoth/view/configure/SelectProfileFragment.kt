package org.rfcx.audiomoth.view.configure


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
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.util.FirestoreResponseCallback
import org.rfcx.audiomoth.view.DeploymentProtocol

class SelectProfileFragment : Fragment(), (Profile) -> Unit {
    private val profilesAdapter by lazy { ProfilesAdapter(this) }
    private var deploymentProtocol: DeploymentProtocol? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_profile, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as DeploymentProtocol
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        retrieveProfiles()
    }

    // @{ProfilesAdapter.itemClickListener}
    override fun invoke(profile: Profile) {
        deploymentProtocol?.openConfigure(profile)
    }

    private fun setupView() {
        deploymentProtocol?.hideCompleteButton()

        createNewButton.setOnClickListener {
            deploymentProtocol?.openConfigure(Profile.default())
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
        Firestore().getProfiles(object : FirestoreResponseCallback<List<Profile?>?> {
            override fun onSuccessListener(response: List<Profile?>?) {
                val items = arrayListOf<Profile>()
                response?.forEach {
                    if (it != null) {
                        items.add(it)
                    }
                }
                profilesAdapter.items = items
                checkState(SHOW_LIST_PROFILE)
            }

            override fun addOnFailureListener(exception: Exception) {
                checkState(SHOW_TRY_AGAIN)
            }
        })
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