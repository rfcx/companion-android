package org.rfcx.companion.view.profile.offlinemap

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.*
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_offline_map.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.localdb.ProjectDb
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.isNetworkAvailable
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
    lateinit var definition: OfflineTilePyramidRegionDefinition

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

    private fun offlineMapBox(project: Project) {
        if (context.isNetworkAvailable()) {
            val offlineManager: OfflineManager? = context?.let { OfflineManager.getInstance(it) }
            val minLat = project.minLatitude
            val maxLat = project.maxLatitude
            val minLng = project.minLongitude
            val maxLng = project.maxLongitude

            offlineManager?.setOfflineMapboxTileCountLimit(10000)
            val style = Style.OUTDOORS
            if (minLat !== null && maxLat !== null && minLng !== null && maxLng !== null) {
                val latLngBounds: LatLngBounds = LatLngBounds.from(
                    maxLat.toDouble(),
                    maxLng.toDouble(),
                    minLat.toDouble(),
                    minLng.toDouble()
                )
                definition = OfflineTilePyramidRegionDefinition(
                    style,
                    latLngBounds,
                    10.0,
                    15.0,
                    context?.resources?.displayMetrics?.density ?: 0.0F
                )
                val regionName = "{\"regionName\":\"${project.name}\"}"
                offlineManager?.createOfflineRegion(definition, regionName.toByteArray(),
                    object : OfflineManager.CreateOfflineRegionCallback {
                        override fun onCreate(offlineRegion: OfflineRegion) {
                            createOfflineRegion(offlineRegion)
                        }

                        override fun onError(error: String) {
                            Log.e(TAG, "Error: $error")
                        }
                    })
            }
        } else {
            Toast.makeText(context, "no_internet_connection", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createOfflineRegion(offlineRegion: OfflineRegion) {
        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
        offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
            private var percentage: Int = -1

            override fun onStatusChanged(status: OfflineRegionStatus) {
                val required = status.requiredResourceCount
                val oldPercentage = this.percentage
                val megabybtes = status.completedResourceSize / 1048576
                val percentage: Int = when {
                    status.isComplete -> {
                        101
                    }
                    required > 0L ->
                        (100 * status.completedResourceCount / required).toInt()
                    else -> 0
                }
                this.percentage = percentage
                if (percentage > oldPercentage)
                    if (percentage >= 100) {
                        Log.d(TAG, "Done")
                    } else {
                        Log.d(TAG, "$percentage %")
                    }
            }

            override fun onError(error: OfflineRegionError) {
                Log.e(TAG, "Mapbox tile count limit exceeded: $error")
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                Log.e(TAG, "Mapbox tile count limit exceeded: $limit")
            }
        })
    }

    override fun onClicked(group: Project) {}
    override fun onLongClicked(group: Project) {}

    override fun onDownloadClicked(project: Project) {
        offlineMapBox(project)
    }
}
