package org.rfcx.companion.view.profile.offlinemap

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.rfcx.companion.entity.OfflineMapState
import org.rfcx.companion.entity.Project
import org.rfcx.companion.util.asLiveData

class ProjectOfflineMapViewModel(
    application: Application,
    private val projectOfflineMapRepository: ProjectOfflineMapRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    private var projects = MutableLiveData<List<Project>>()
    private var stateOfflineMap = MutableLiveData<String>()
    private var percentageDownloads = MutableLiveData<Int>()
    private var hideDownloadButton = MutableLiveData<Boolean>()
    private lateinit var definition: OfflineTilePyramidRegionDefinition

    private lateinit var projectsLiveData: LiveData<List<Project>>
    private val projectsObserve = Observer<List<Project>> {
        projects.postValue(it)
    }

    init {
        fetchLiveData()
    }

    private fun fetchLiveData() {
        projectsLiveData =
            Transformations.map(
                projectOfflineMapRepository.getAllProjectResultsAsync().asLiveData()
            ) {
                it
            }
        projectsLiveData.observeForever(projectsObserve)
    }

    fun getStateOfflineMap(): LiveData<String> {
        return stateOfflineMap
    }

    fun getPercentageDownloads(): LiveData<Int> {
        return percentageDownloads
    }

    fun hideDownloadButton(): LiveData<Boolean> {
        return hideDownloadButton
    }

    fun getProjects(): LiveData<List<Project>> {
        return projects
    }

    fun getOfflineDownloading(): Project? {
        return projectOfflineMapRepository.getOfflineDownloading()
    }

    fun getProjectsFromLocal(): List<Project> {
        return projectOfflineMapRepository.getProjectsFromLocal()
    }

    fun updateOfflineDownloadedState() {
        projectOfflineMapRepository.updateOfflineDownloadedState()
    }

    fun updateOfflineState(state: String, id: String) {
        projectOfflineMapRepository.updateOfflineState(state, id)
    }

    fun offlineMapBox(project: Project) {
        val offlineManager: OfflineManager? = context?.let { OfflineManager.getInstance(it) }
        val minLat = project.minLatitude
        val maxLat = project.maxLatitude
        val minLng = project.minLongitude
        val maxLng = project.maxLongitude

        stateOfflineMap.postValue(OfflineMapState.DOWNLOADING_STATE.key)

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
                jsonObject.put("regionName", project.name)
                val json = jsonObject.toString()
                json.toByteArray(charset)
            } catch (exception: java.lang.Exception) {
                null
            }

            if (metadata != null) {
                offlineManager?.createOfflineRegion(definition, metadata,
                    object : OfflineManager.CreateOfflineRegionCallback {
                        override fun onCreate(offlineRegion: OfflineRegion) {
                            hideDownloadButton.postValue(true)
                            CoroutineScope(Dispatchers.IO).launch {
                                createOfflineRegion(
                                    offlineRegion,
                                    project
                                )
                            }
                        }

                        override fun onError(error: String) {
                            stateOfflineMap.postValue(OfflineMapState.DOWNLOAD_STATE.key)
                            Log.e(OfflineMapFragment.TAG, "Error: $error")
                        }
                    })
            }
        }
    }

    private fun createOfflineRegion(offlineRegion: OfflineRegion, project: Project) {
        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
        offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
            private var percentageNumber: Int = -1

            override fun onStatusChanged(status: OfflineRegionStatus) {
                val required = status.requiredResourceCount
                val oldPercentage = this.percentageNumber
                val megabybtes = status.completedResourceSize / 1048576
                val percentage: Int = when {
                    status.isComplete -> {
                        101
                    }
                    required > 0L ->
                        (100 * status.completedResourceCount / required).toInt()
                    else -> 0
                }
                this.percentageNumber = percentage
                if (percentage > oldPercentage) {
                    percentageDownloads.postValue(percentage)
                }
            }

            override fun onError(error: OfflineRegionError) {
                stateOfflineMap.postValue(OfflineMapState.DOWNLOAD_STATE.key)
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                Log.e(OfflineMapFragment.TAG, "Mapbox tile count limit exceeded: $limit")
            }
        })
    }

    fun deleteOfflineRegion(project: Project) {
        stateOfflineMap.postValue(OfflineMapState.DELETING_STATE.key)

        val offlineManager: OfflineManager? = context?.let { OfflineManager.getInstance(it) }
        offlineManager?.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<out OfflineRegion>?) {
                if (!offlineRegions.isNullOrEmpty()) {
                    offlineRegions.map {
                        if (getRegionName(it) == project.name) {
                            hideDownloadButton.postValue(true)
                            onDeleteOfflineRegion(it, project)
                        }
                    }
                }
            }

            override fun onError(error: String?) {
                stateOfflineMap.postValue(OfflineMapState.DOWNLOADED_STATE.key)
                updateOfflineState(OfflineMapState.DOWNLOADED_STATE.key, project.serverId ?: "")
            }
        })
    }

    fun onDeleteOfflineRegion(offRegion: OfflineRegion, project: Project) {
        offRegion.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
            override fun onDelete() {
                stateOfflineMap.postValue(OfflineMapState.DOWNLOAD_STATE.key)
                updateOfflineState(OfflineMapState.DOWNLOAD_STATE.key, project.serverId ?: "")
            }

            override fun onError(error: String) {
                stateOfflineMap.postValue(OfflineMapState.DOWNLOADED_STATE.key)
                updateOfflineState(OfflineMapState.DOWNLOADED_STATE.key, project.serverId ?: "")
            }
        })
    }

    private fun getRegionName(offlineRegion: OfflineRegion): String {
        val metadata = offlineRegion.metadata
        val json = String(metadata)
        val jsonObject = JSONObject(json)
        return jsonObject.getString("regionName")
    }

    fun onDestroy() {
        projectsLiveData.removeObserver(projectsObserve)
    }
}
