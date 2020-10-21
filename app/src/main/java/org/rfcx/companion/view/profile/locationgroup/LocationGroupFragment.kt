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
import org.rfcx.companion.entity.LocationGroups
import org.rfcx.companion.localdb.DatabaseCallback
import org.rfcx.companion.localdb.LocationGroupDb
import org.rfcx.companion.service.LocationGroupSyncWorker
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.showCommonDialog

class LocationGroupFragment : Fragment(), LocationGroupListener {
    val realm: Realm = Realm.getInstance(RealmHelper.migrationConfig())
    private val locationGroupDb = LocationGroupDb(realm)

    private val locationGroupAdapter by lazy { LocationGroupAdapter(this) }
    private var locationGroupProtocol: LocationGroupProtocol? = null
    private var selectedGroup: String? = null

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
            locationGroupProtocol?.onCreateNewGroup()
        }
        locationGroupAdapter.items = locationGroupDb.getLocationGroups()
        locationGroupAdapter.selectedGroup = selectedGroup
    }

    private fun initIntent() {
        arguments?.let { selectedGroup = it.getString(ARG_GROUP) }
    }

    override fun onClicked(group: LocationGroups) {
        locationGroupProtocol?.onLocationGroupClick(group)
    }

    override fun onLongClicked(group: LocationGroups) {
        showDeleteDialog(group)
    }

    private fun showDeleteDialog(group: LocationGroups) {
        val builder = context?.let { it1 -> AlertDialog.Builder(it1, R.style.DialogCustom) }
        builder?.apply {
            setTitle(getString(R.string.delete_location_group_title, group.name))
            setPositiveButton(getString(R.string.delete)) { dialog, which ->
                locationGroupDb.deleteLocationGroup(group.id, object : DatabaseCallback {
                    override fun onSuccess() {
                        LocationGroupSyncWorker.enqueue(requireActivity())
                        locationGroupAdapter.removeGroup(group.id)
                        locationGroupAdapter.notifyDataSetChanged()
                    }
                    override fun onFailure(errorMessage: String) {
                        requireActivity().showCommonDialog(errorMessage)
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
        locationGroupAdapter.items = locationGroupDb.getLocationGroups()
    }

    companion object {
        private const val ARG_GROUP = "ARG_GROUP"

        @JvmStatic
        fun newInstance(group: String?) = LocationGroupFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_GROUP, group)
            }
        }
    }

}
