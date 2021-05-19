package org.rfcx.companion.view.profile.offlinemap

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_offline_map.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.localdb.ProjectDb
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.view.profile.locationgroup.LocationGroupAdapter
import org.rfcx.companion.view.profile.locationgroup.LocationGroupListener

class OfflineMapFragment : Fragment(), LocationGroupListener {

    companion object {
        const val TAG = "OfflineMapFragment"

        @JvmStatic
        fun newInstance() = OfflineMapFragment()
    }

    // database manager
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val projectDb by lazy { ProjectDb(realm) }

    private val projectAdapter by lazy { LocationGroupAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_offline_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
    }

    private fun setupAdapter() {
        projectsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = projectAdapter
        }
        projectAdapter.screen = Screen.OFFLINE_MAP.id
        projectAdapter.items = projectDb.getProjects()
    }

    override fun onClicked(group: Project) {
        Log.d(TAG,"onClicked ${group.name}")
    }

    override fun onLongClicked(group: Project) {
        Log.d(TAG,"onLongClicked")
    }
}
