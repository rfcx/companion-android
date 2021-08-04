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
import androidx.lifecycle.ViewModelProvider
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
import org.json.JSONObject
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.OfflineMapState
import org.rfcx.companion.entity.Project
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.isNetworkAvailable

class OfflineMapFragment : Fragment(), ProjectOfflineMapListener {

    companion object {
        const val TAG = "OfflineMapFragment"

        // JSON encoding/decoding
        const val JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME"

        @JvmStatic
        fun newInstance() = OfflineMapFragment()
    }
    private lateinit var projectOfflineMapViewModel: ProjectOfflineMapViewModel

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

        setViewModel()
        setObserver()
        setupAdapter()
    }

    private fun setViewModel() {
        projectOfflineMapViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                requireActivity().application,
                DeviceApiHelper(DeviceApiServiceImpl()),
                LocalDataHelper()
            )
        ).get(ProjectOfflineMapViewModel::class.java)
    }

    private fun setObserver() {
        projectOfflineMapViewModel.getProjected().observe(viewLifecycleOwner, Observer {
            if (projectOfflineMapViewModel.getOfflineDownloading() == null) {
                projectAdapter.hideDownloadButton = projectOfflineMapViewModel.getOfflineDownloading() != null
            }
        })
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
            projectAdapter = ProjectOfflineMapAdapter(projectOfflineMapViewModel.getProjectsFromLocal(), this@OfflineMapFragment)
            adapter = projectAdapter
        }
        projectAdapter.hideDownloadButton = projectOfflineMapViewModel.getOfflineDownloading() != null

        if (projectOfflineMapViewModel.getOfflineDownloading() != null) {
            offlineMapBox(projectOfflineMapViewModel.getOfflineDownloading()!!)
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

                val metadata: ByteArray? = try {
                    val jsonObject = JSONObject()
                    val charset = Charsets.UTF_8
                    jsonObject.put(JSON_FIELD_REGION_NAME, project.name)
                    val json = jsonObject.toString()
                    json.toByteArray(charset)
                } catch (exception: java.lang.Exception) {
                    null
                }

                if (metadata != null) {
                    offlineManager?.createOfflineRegion(definition, metadata,
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
            }
        } else {
            Toast.makeText(context, context?.getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
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
                        projectOfflineMapViewModel.updateOfflineDownloadedState()
                        setStateOfflineMap(OfflineMapState.DOWNLOADED_STATE.key)
                        projectAdapter.setDownloading(project)
                    } else {
                        projectAdapter.setProgress(project, percentage)
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
        projectOfflineMapViewModel.updateOfflineState(OfflineMapState.DOWNLOADING_STATE.key, project.serverId ?: "")

        offlineMapBox(project)
    }

    override fun onDeleteClicked(project: Project) {
        val preferences = context?.let { Preferences.getInstance(it) }
        preferences?.putString(Preferences.OFFLINE_MAP_SERVER_ID, project.serverId ?: "")
        projectOfflineMapViewModel.updateOfflineState(OfflineMapState.DELETING_STATE.key, project.serverId ?: "")

        deleteOfflineRegion(project)
    }

    private fun deleteOfflineRegion(project: Project) {
        if (context.isNetworkAvailable()) {
            setStateOfflineMap(OfflineMapState.DELETING_STATE.key)

            val offlineManager: OfflineManager? = context?.let { OfflineManager.getInstance(it) }
            offlineManager?.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<out OfflineRegion>?) {
                    if (!offlineRegions.isNullOrEmpty()) {
                        offlineRegions.map {
                            if (getRegionName(it) == project.name) {
                                projectAdapter.hideDownloadButton = true
                                onDeleteOfflineRegion(it, project)
                            }
                        }
                    }
                }

                override fun onError(error: String?) {
                    setStateOfflineMap(OfflineMapState.DOWNLOADED_STATE.key)
                    projectOfflineMapViewModel.updateOfflineState(OfflineMapState.DOWNLOADED_STATE.key, project.serverId ?: "")
                }
            })
        } else {
            Toast.makeText(context, context?.getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
        }
    }

    fun onDeleteOfflineRegion(offRegion: OfflineRegion, project: Project) {
        offRegion.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
            override fun onDelete() {
                setStateOfflineMap(OfflineMapState.DOWNLOAD_STATE.key)
                projectOfflineMapViewModel.updateOfflineState(OfflineMapState.DOWNLOAD_STATE.key, project.serverId ?: "")
            }

            override fun onError(error: String) {
                setStateOfflineMap(OfflineMapState.DOWNLOADED_STATE.key)
                projectOfflineMapViewModel.updateOfflineState(OfflineMapState.DOWNLOADED_STATE.key, project.serverId ?: "")
            }
        })
    }

    private fun getRegionName(offlineRegion: OfflineRegion): String? {
        // Get the region name from the offline region metadata
        return try {
            val metadata = offlineRegion.metadata
            val json = String(metadata)
            val jsonObject = JSONObject(json)
            jsonObject.getString(JSON_FIELD_REGION_NAME)
        } catch (exception: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        projectOfflineMapViewModel.onDestroy()
    }
}
