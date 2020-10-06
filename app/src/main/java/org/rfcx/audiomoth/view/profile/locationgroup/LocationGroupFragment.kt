package org.rfcx.audiomoth.view.profile.locationgroup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_location_group.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.LocationGroups
import org.rfcx.audiomoth.localdb.LocationGroupDb
import org.rfcx.audiomoth.util.RealmHelper

class LocationGroupFragment : Fragment(), (LocationGroups) -> Unit {
    val realm: Realm = Realm.getInstance(RealmHelper.migrationConfig())
    private val locationGroupDb = LocationGroupDb(realm)

    private val locationGroupAdapter by lazy { LocationGroupAdapter(this) }
    private var locationGroupProtocol: LocationGroupProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        locationGroupProtocol = (context as LocationGroupProtocol)
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
    }

    override fun invoke(group: LocationGroups) {
        locationGroupProtocol?.onLocationGroupClick(group)
    }

    override fun onResume() {
        super.onResume()
        locationGroupAdapter.items = locationGroupDb.getLocationGroups()
    }

    companion object {
        @JvmStatic
        fun newInstance() = LocationGroupFragment()
    }
}
