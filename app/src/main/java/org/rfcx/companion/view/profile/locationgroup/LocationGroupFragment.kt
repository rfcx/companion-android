package org.rfcx.companion.view.profile.locationgroup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_location_group.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.Status
import org.rfcx.companion.localdb.DatabaseCallback
import org.rfcx.companion.localdb.ProjectDb
import org.rfcx.companion.service.LocationGroupSyncWorker
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.showCommonDialog

class LocationGroupFragment : Fragment(), LocationGroupListener {
    val realm: Realm = Realm.getInstance(RealmHelper.migrationConfig())
    private val locationGroupDb = ProjectDb(realm)

    private val locationGroupAdapter by lazy { LocationGroupAdapter(this) }
    private var locationGroupProtocol: LocationGroupProtocol? = null
    private var selectedGroup: String? = null
    private var screen: String? = null
    private val analytics by lazy { context?.let { Analytics(it) } }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        locationGroupProtocol = (context as LocationGroupProtocol)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_location_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationGroupRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = locationGroupAdapter
        }

        locationGroupLinearLayout.setOnClickListener {
            analytics?.trackCreateNewGroupEvent()
            locationGroupProtocol?.onCreateNewGroup()
        }
        locationGroupAdapter.selectedGroup = selectedGroup ?: getString(R.string.none)
        locationGroupAdapter.screen = screen ?: Screen.PROFILE.id
    }

    private fun initIntent() {
        arguments?.let {
            selectedGroup = it.getString(ARG_GROUP)
            screen = it.getString(LocationGroupActivity.EXTRA_SCREEN)
        }
    }

    override fun onClicked(group: Project) {
        locationGroupProtocol?.onLocationGroupClick(group)
    }

    override fun onLongClicked(group: Project) {
//        showDeleteDialog(group)
    }

    override fun onDownloadClicked(project: Project) {}

    private fun showDeleteDialog(group: Project) {
        val preferences = context?.let { Preferences.getInstance(it) }
        val groupName = preferences?.getString(Preferences.GROUP, getString(R.string.none))

        val builder = context?.let { it1 -> AlertDialog.Builder(it1, R.style.DialogCustom) }
        builder?.apply {
            setTitle(getString(R.string.delete_location_group_title))
            setPositiveButton(getString(R.string.delete)) { dialog, which ->
                locationGroupDb.deleteProject(group.id, object : DatabaseCallback {
                    override fun onSuccess() {
                        if(group.name == groupName) {
                            preferences?.putString(Preferences.GROUP, getString(R.string.none))
                        }
                        LocationGroupSyncWorker.enqueue(requireActivity())
                        locationGroupAdapter.removeGroup(group.id)
                        locationGroupAdapter.notifyDataSetChanged()
                        analytics?.trackDeleteLocationGroupEvent(Status.SUCCESS.id)
                    }
                    override fun onFailure(errorMessage: String) {
                        requireActivity().showCommonDialog(errorMessage)
                        analytics?.trackDeleteLocationGroupEvent(Status.FAILURE.id)
                    }
                })
                dialog.dismiss()
            }
            setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                dialog.dismiss()
            }
        }
        builder?.show()
    }

    override fun onResume() {
        super.onResume()
        locationGroupAdapter.items = listOf(
            Project(
                id = -1,
                name = getString(R.string.none)
            )
        ) + locationGroupDb.getProjects()
    }

    companion object {
        private const val ARG_GROUP = "ARG_GROUP"

        @JvmStatic
        fun newInstance(group: String?, screen: String?) = LocationGroupFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_GROUP, group)
                putString(LocationGroupActivity.EXTRA_SCREEN, screen)
            }
        }
    }

}
