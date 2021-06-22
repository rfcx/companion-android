package org.rfcx.companion.view.profile.offlinemap

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.*
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_offline_map.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.rfcx.companion.R
import org.rfcx.companion.entity.OfflineMapState
import org.rfcx.companion.entity.Project
import org.rfcx.companion.localdb.ProjectDb
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.asLiveData
import org.rfcx.companion.util.isNetworkAvailable

class OfflineMapFragment : Fragment(), ProjectOfflineMapListener {

    companion object {
        const val TAG = "OfflineMapFragment"

        @JvmStatic
        fun newInstance() = OfflineMapFragment()
    }

    // database manager
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val projectDb by lazy { ProjectDb(realm) }

    private lateinit var projectLiveData: LiveData<List<Project>>
    private val projectObserve = Observer<List<Project>> {
        if (projectDb.getOfflineDownloading() == null) {
            projectAdapter.hideDownloadButton = projectDb.getOfflineDownloading() != null
        }

    }

    lateinit var definition: OfflineTilePyramidRegionDefinition
    lateinit var projectAdapter: ProjectOfflineMapAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_offline_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val job = Job()
        val scope = CoroutineScope(Dispatchers.IO + job)
        scope.launch {
            job.cancel()
        }

        setupAdapter()
        projectLiveData =
            Transformations.map(projectDb.getAllResultsAsync().asLiveData()) {
                it
            }
        projectLiveData.observeForever(projectObserve)
    }

    private fun setupAdapter() {
        with(projectsRecyclerView) {
            layoutManager = LinearLayoutManager(context)
            DividerItemDecoration(
                context,
                (layoutManager as LinearLayoutManager).orientation
            ).apply {
                addItemDecoration(this)
            }
            val data = projectDb.getProjects().map { OfflineMapItem(it) }

            projectAdapter = ProjectOfflineMapAdapter(data, this@OfflineMapFragment)
            adapter = projectAdapter
        }
        projectAdapter.hideDownloadButton = projectDb.getOfflineDownloading() != null

        if (projectDb.getOfflineDownloading() != null) {
            offlineMapBox(projectDb.getOfflineDownloading()!!)
        }
    }

    private fun offlineMapBox(project: Project) {
        if (context.isNetworkAvailable()) {
            val offlineManager: OfflineManager? = context?.let { OfflineManager.getInstance(it) }
            val minLat = project.minLatitude
            val maxLat = project.maxLatitude
            val minLng = project.minLongitude
            val maxLng = project.maxLongitude

            setStateOfflineMap(OfflineMapState.DOWNLOADING_STATE.key)

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
                            projectAdapter.hideDownloadButton = true
                            CoroutineScope(Dispatchers.IO).launch { createOfflineRegion(offlineRegion, project) }
                        }

                        override fun onError(error: String) {
                            setStateOfflineMap(OfflineMapState.DOWNLOAD_STATE.key)
                            Log.e(TAG, "Error: $error")
                        }
                    })
            }
        } else {
            Toast.makeText(context, "no_internet_connection", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createOfflineRegion(offlineRegion: OfflineRegion, project: Project) {
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
                        projectDb.updateOfflineDownloadedState()
                        setStateOfflineMap(OfflineMapState.DOWNLOADED_STATE.key)
                        projectAdapter.setDownloading(OfflineMapItem(project))
                    } else {
                        projectAdapter.setProgress(OfflineMapItem(project, percentage), percentage)
                        setStateOfflineMap(OfflineMapState.DOWNLOADING_STATE.key)
                    }
            }

            override fun onError(error: OfflineRegionError) {
                setStateOfflineMap(OfflineMapState.DOWNLOAD_STATE.key)
                Log.e(TAG, "Mapbox tile count limit exceeded: $error")
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                Log.e(TAG, "Mapbox tile count limit exceeded: $limit")
            }
        })
    }

    private fun setStateOfflineMap(state: String) {
        val preferences = context?.let { Preferences.getInstance(it) }
        preferences?.putString(Preferences.OFFLINE_MAP_STATE, state)
    }

    override fun onDownloadClicked(project: Project) {
        val preferences = context?.let { Preferences.getInstance(it) }
        preferences?.putString(Preferences.OFFLINE_MAP_SERVER_ID, project.serverId ?: "")
        projectDb.updateOfflineState(OfflineMapState.DOWNLOADING_STATE.key, project.serverId ?: "")

        offlineMapBox(project)
    }

    override fun onDestroy() {
        super.onDestroy()
        projectLiveData.removeObserver(projectObserve)
    }
}
