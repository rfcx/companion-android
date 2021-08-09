package org.rfcx.companion

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition
import io.realm.RealmResults
import org.json.JSONObject
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.entity.guardian.toMark
import org.rfcx.companion.entity.response.DeploymentAssetResponse
import org.rfcx.companion.entity.response.ProjectByIdResponse
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.service.DownloadStreamsWorker
import org.rfcx.companion.util.*
import org.rfcx.companion.util.geojson.GeoJsonUtils
import org.rfcx.companion.view.map.MapMarker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class MainViewModel(
    application: Application,
    private val mainRepository: MainRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private val projects = MutableLiveData<Resource<List<Project>>>()
    private val tracks = MutableLiveData<Resource<List<DeploymentAssetResponse>>>()
    private val deploymentMarkers = MutableLiveData<Resource<List<MapMarker.DeploymentMarker>>>()
    private val siteMarkers = MutableLiveData<Resource<List<MapMarker>>>()
    private val siteList = MutableLiveData<Resource<List<Locate>>>()
    private val showDeployments = MutableLiveData<Resource<List<EdgeDeployment>>>()
    private val showGuardianDeployments = MutableLiveData<Resource<List<GuardianDeployment>>>()

    private var deployments = listOf<GuardianDeployment>()
    private var sites = listOf<Locate>()

    private lateinit var deploymentLiveData: LiveData<List<GuardianDeployment>>
    private val deploymentObserve = Observer<List<GuardianDeployment>> {
        deployments = it
        combinedData()
    }

    private lateinit var siteLiveData: LiveData<List<Locate>>
    private val siteObserve = Observer<List<Locate>> {
        sites = it
        siteList.postValue(Resource.success(it))
        combinedData()
    }

    init {
        fetchLiveData()
    }

    private fun fetchLiveData() {
        siteLiveData =
            Transformations.map(mainRepository.getAllLocateResultsAsync().asLiveData()) { it }
        siteLiveData.observeForever(siteObserve)

        deploymentLiveData = Transformations.map(
            mainRepository.getAllGuardianDeploymentLocateResultsAsync().asLiveData()
        ) { it }
        deploymentLiveData.observeForever(deploymentObserve)
    }

    fun fetchProjects() {
        projects.postValue(Resource.loading(null))
        mainRepository.getProjectsFromRemote("Bearer ${context.getIdToken()}")
            .enqueue(object : Callback<List<ProjectResponse>> {
                override fun onFailure(call: Call<List<ProjectResponse>>, t: Throwable) {
                    if (context.isNetworkAvailable()) {
                        projects.postValue(
                            Resource.error(
                                context.getString(R.string.network_not_available),
                                null
                            )
                        )
                    }
                }

                override fun onResponse(
                    call: Call<List<ProjectResponse>>,
                    response: Response<List<ProjectResponse>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { projectsRes ->
                            if (!projectsRes.isNullOrEmpty()) {
                                projectsRes.forEach { item ->
                                    mainRepository.saveProjectToLocal(item)
                                }
                                fetchDeletedProjects()
                            } else {
                                projects.postValue(Resource.success(listOf()))
                            }
                        }
                    } else {
                        projects.postValue(
                            Resource.error(
                                context.getString(R.string.something_went_wrong),
                                null
                            )
                        )
                    }
                }
            })
    }

    fun updateProjectBounds() {
        val updateProjectBounds =
            getProjectsFromLocal().filter { project -> project.serverId != null && project.maxLatitude == null }
        updateProjectBounds.map { projectBounds ->
            val token = "Bearer ${context?.getIdToken()}"
            projectBounds.serverId?.let { serverId ->
                mainRepository.getProjectsByIdFromCore(token, serverId)
                    .enqueue(object : Callback<ProjectByIdResponse> {
                        override fun onFailure(call: Call<ProjectByIdResponse>, t: Throwable) {}
                        override fun onResponse(
                            call: Call<ProjectByIdResponse>,
                            response: Response<ProjectByIdResponse>
                        ) {
                            if (response.body() != null && response.body()?.id != null && response.body()?.maxLatitude != null) {
                                val res = response.body() as ProjectByIdResponse
                                updateProjectBounds(res)
                                updateStatusOfflineMap()
                            }
                        }
                    })
            }
        }
    }

    private fun fetchDeletedProjects() {
        mainRepository.getDeletedProjectsFromRemote("Bearer ${context.getIdToken()}")
            .enqueue(object : Callback<List<ProjectResponse>> {
                override fun onFailure(call: Call<List<ProjectResponse>>, t: Throwable) {
                    if (context.isNetworkAvailable()) {
                        projects.postValue(
                            Resource.error(
                                context.getString(R.string.network_not_available),
                                null
                            )
                        )
                    }
                }

                override fun onResponse(
                    call: Call<List<ProjectResponse>>,
                    response: Response<List<ProjectResponse>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { projectsRes ->
                            mainRepository.removeProjectFromLocal(projectsRes.map { it.id!! }) // remove project with these coreIds
                            projects.postValue(Resource.success(null)) // no need to send project data
                        }
                    } else {
                        projects.postValue(
                            Resource.error(
                                context.getString(R.string.something_went_wrong),
                                null
                            )
                        )
                    }
                }
            })
    }

    fun getStreamAssets(site: Locate) {
        tracks.postValue(Resource.loading(null))
        mainRepository.getStreamAssets("Bearer ${context.getIdToken()}", site.serverId!!)
            .enqueue(object : Callback<List<DeploymentAssetResponse>> {
                override fun onResponse(
                    call: Call<List<DeploymentAssetResponse>>,
                    response: Response<List<DeploymentAssetResponse>>
                ) {
                    var fileCount = 0
                    var fileCreated = 0
                    val siteAssets = response.body()
                    siteAssets?.forEach { item ->
                        if (item.mimeType.endsWith("geo+json")) {
                            fileCount += 1
                            GeoJsonUtils.downloadGeoJsonFile(
                                context,
                                "Bearer ${context.getIdToken()}",
                                item,
                                site.serverId!!,
                                Date(),
                                object : GeoJsonUtils.DownloadTrackCallback {
                                    override fun onSuccess(filePath: String) {
                                        fileCreated += 1
                                        mainRepository.saveTrackingToLocal(
                                            item,
                                            filePath,
                                            site.id
                                        )
                                        if (fileCount == fileCreated) {
                                            tracks.postValue(Resource.success(response.body()))
                                        }
                                    }

                                    override fun onFailed(msg: String) {
                                        tracks.postValue(
                                            Resource.error(msg, null)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                override fun onFailure(call: Call<List<DeploymentAssetResponse>>, t: Throwable) {
                    tracks.postValue(
                        Resource.error(
                            context.getString(R.string.something_went_wrong),
                            null
                        )
                    )
                }
            })
    }

    fun combinedData() {
        var deploymentsForShow = this.deployments.filter { it.isCompleted() }
        val usedSitesOnGuardian = deploymentsForShow.map { it.stream?.coreId }
        var filteredShowLocations =
            sites.filter { loc -> !usedSitesOnGuardian.contains(loc.serverId) || (loc.serverId == null && (loc.lastDeploymentId == 0 && loc.lastGuardianDeploymentId == 0)) }

        val projectName = getProjectName()
        if (projectName != context.getString(R.string.none)) {
            filteredShowLocations =
                filteredShowLocations.filter { it.locationGroup?.name == projectName }
            deploymentsForShow =
                deploymentsForShow.filter { it.stream?.project?.name == projectName }
        }
        val guardianDeploymentMarkers = deploymentsForShow.map { it.toMark(context) }
        deploymentMarkers.postValue(Resource.success(guardianDeploymentMarkers))
        siteMarkers.postValue(Resource.success(filteredShowLocations.map { it.toMark() }))
        showGuardianDeployments.postValue(Resource.success(deploymentsForShow))
    }

    fun updateStatusOfflineMap() {
        getProjectsFromLocal().map { project ->
            if (context.isNetworkAvailable()) {
                val offlineManager: OfflineManager? =
                    context?.let { OfflineManager.getInstance(it) }
                val definition: OfflineTilePyramidRegionDefinition
                offlineManager?.setOfflineMapboxTileCountLimit(10000)

                val minLat = project.minLatitude
                val maxLat = project.maxLatitude
                val minLng = project.minLongitude
                val maxLng = project.maxLongitude

                if (minLat !== null && maxLat !== null && minLng !== null && maxLng !== null) {
                    val regionName = "{\"regionName\":\"regionName.${project.name}\"}"
                    val latLngBounds: LatLngBounds = LatLngBounds.from(
                        maxLat.toDouble(),
                        maxLng.toDouble(),
                        minLat.toDouble(),
                        minLng.toDouble()
                    )
                    definition = OfflineTilePyramidRegionDefinition(
                        Style.OUTDOORS,
                        latLngBounds,
                        10.0,
                        15.0,
                        context?.resources?.displayMetrics?.density ?: 0.0F
                    )

                    offlineManager?.createOfflineRegion(definition, regionName.toByteArray(),
                        object : OfflineManager.CreateOfflineRegionCallback {
                            override fun onCreate(offlineRegion: OfflineRegion) {
                                offlineRegion.getStatus(object :
                                    OfflineRegion.OfflineRegionStatusCallback {
                                    override fun onStatus(status: OfflineRegionStatus?) {
                                        if (status == null) return
                                        if (status.requiredResourceCount > 10000) {
                                            mainRepository.updateOfflineState(
                                                OfflineMapState.UNAVAILABLE.key,
                                                project.serverId ?: ""
                                            )
                                        }
                                        deleteOfflineRegion(project, offlineManager)
                                    }

                                    override fun onError(error: String?) {}
                                })
                            }

                            override fun onError(error: String) {}
                        })
                }
            }
        }
    }

    private fun deleteOfflineRegion(project: Project, offlineManager: OfflineManager) {
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<out OfflineRegion>?) {
                if (!offlineRegions.isNullOrEmpty()) {
                    offlineRegions.map {
                        if (getRegionName(it) == "regionName.${project.name}") {
                            it.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                                override fun onDelete() {}
                                override fun onError(error: String) {}
                            })
                        }
                    }
                }
            }

            override fun onError(error: String?) {}
        })
    }

    private fun getRegionName(offlineRegion: OfflineRegion): String {
        val metadata = offlineRegion.metadata
        val json = String(metadata)
        val jsonObject = JSONObject(json)
        return jsonObject.getString("regionName")
    }

    private fun getProjectName(): String {
        val preferences = Preferences.getInstance(context)
        val projectId = preferences.getInt(Preferences.SELECTED_PROJECT)
        val project = mainRepository.getProjectById(projectId)
        return project?.name ?: context.getString(R.string.none)
    }

    fun retrieveLocations() {
        val projectId = Preferences.getInstance(context).getInt(Preferences.SELECTED_PROJECT)
        getProjectById(projectId)?.serverId?.let {
            DownloadStreamsWorker.enqueue(context, it)
        }
    }

    fun getProjectsFromRemote(): LiveData<Resource<List<Project>>> {
        return projects
    }

    fun getDeploymentMarkers(): LiveData<Resource<List<MapMarker.DeploymentMarker>>> {
        return deploymentMarkers
    }

    fun getSiteMarkers(): LiveData<Resource<List<MapMarker>>> {
        return siteMarkers
    }

    fun getSites(): LiveData<Resource<List<Locate>>> {
        return siteList
    }

    fun getShowDeployments(): LiveData<Resource<List<EdgeDeployment>>> {
        return showDeployments
    }

    fun getShowGuardianDeployments(): LiveData<Resource<List<GuardianDeployment>>> {
        return showGuardianDeployments
    }

    fun getTrackingFromRemote(): LiveData<Resource<List<DeploymentAssetResponse>>> {
        return tracks
    }

    fun getProjectsFromLocal(): List<Project> {
        return mainRepository.getProjectsFromLocal()
    }

    fun getProjectById(id: Int): Project? {
        return mainRepository.getProjectById(id)
    }

    fun getDeploymentUnsentCount(): Int {
        return mainRepository.getDeploymentUnsentCount()
    }

    fun getGuardianDeploymentById(id: Int): GuardianDeployment? {
        return mainRepository.getGuardianDeploymentById(id)
    }

    fun getLocateByName(name: String): Locate? {
        return mainRepository.getLocateByName(name)
    }

    fun getLocateById(id: Int): Locate? {
        return mainRepository.getLocateById(id)
    }

    fun getTrackingFileBySiteId(id: Int): RealmResults<TrackingFile> {
        return mainRepository.getTrackingFileBySiteId(id)
    }

    fun getFirstTracking(): Tracking? {
        return mainRepository.getFirstTracking()
    }

    fun deleteTracking(id: Int, context: Context) {
        mainRepository.deleteTracking(id, context)
    }

    fun updateProjectBounds(response: ProjectByIdResponse) {
        mainRepository.updateProjectBounds(response)
    }

    fun onDestroy() {
        deploymentLiveData.removeObserver(deploymentObserve)
        siteLiveData.removeObserver(siteObserve)
    }
}
