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
import org.rfcx.audiomoth.view.DeploymentListener
import org.rfcx.audiomoth.view.DeploymentProtocol
import org.rfcx.audiomoth.view.UserListener

class SelectProfileFragment : Fragment() {
    private val profilesAdapter by lazy { ProfilesAdapter() }
    private var deploymentProtocol: DeploymentProtocol? = null
    private var deploymentListener: DeploymentListener? = null
    private var userListener: UserListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_profile, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as DeploymentProtocol
        deploymentListener = context as DeploymentListener
        userListener = context as UserListener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.hideCompleteButton()

        createNewButton.setOnClickListener {
            deploymentListener?.openConfigure()
        }

        profileRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = profilesAdapter
        }

        getProfile(userListener?.getUserId())

        profilesAdapter.mOnItemClickListener = object : OnItemProfileClickListener {
            override fun onItemClick(profileName: String) {
                deploymentListener?.openConfigure()
            }
        }
    }

    private fun getProfile(documentId: String?) {

        if (documentId != null) {
            Firestore().getProfiles(documentId,
                object : FirestoreResponseCallback<List<Profile?>?> {
                    override fun onSuccessListener(response: List<Profile?>?) {
                        val items = arrayListOf<Profile>()
                        response?.map {
                            if (it != null) {
                                items.add(it)
                            }
                        }
                        profilesAdapter.items = items
                    }

                    override fun addOnFailureListener(exception: Exception) {}
                })
        }

    }

    companion object {
        fun newInstance(): SelectProfileFragment {
            return SelectProfileFragment()
        }
    }
}

interface OnItemProfileClickListener {
    fun onItemClick(profileName: String)
}