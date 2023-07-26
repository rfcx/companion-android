package org.rfcx.companion

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.BaseCallback
import com.auth0.android.result.Credentials
import io.realm.RealmResults
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.guardian.toMark
import org.rfcx.companion.entity.response.DeploymentAssetResponse
import org.rfcx.companion.entity.response.ProjectByIdResponse
import org.rfcx.companion.entity.response.ProjectOffTimeResponse
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.service.DownloadStreamsWorker
import org.rfcx.companion.util.*
import org.rfcx.companion.util.geojson.GeoJsonUtils
import org.rfcx.companion.view.map.MapMarker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainViewModel(
    application: Application,
    private val mainRepository: MainRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private val projects = MutableLiveData<Resource<List<Project>>>()
    private val tracks = MutableLiveData<Resource<List<DeploymentAssetResponse>>>()
    private val unsyncedWorksCount = MutableLiveData<Int>()
    private val deploymentMarkers = MutableLiveData<List<MapMarker.DeploymentMarker>>()
    private val streamMarkers = MutableLiveData<List<MapMarker>>()
    private val streamList = MutableLiveData<List<Stream>>()

    private var streams = listOf<Stream>()

    private lateinit var streamLiveData: LiveData<List<Stream>>
    private val streamObserve = Observer<List<Stream>> {
        streams = it
        combinedData()
    }

    private lateinit var deploymentLiveData: LiveData<List<Deployment>>
    private val deploymentObserve = Observer<List<Deployment>> {
        combinedData()
    }

    private val auth0 by lazy {
        val auth0 =
            Auth0(
                context.getString(R.string.auth0_client_id),
                context.getString(R.string.auth0_domain)
            )
        auth0.isOIDCConformant = true
        auth0.isLoggingEnabled = true
        auth0
    }

    private val authentication by lazy {
        AuthenticationAPIClient(auth0)
    }

    init {
        fetchLiveData()
    }

    private fun fetchLiveData() {
        streamLiveData =
            Transformations.map(mainRepository.getAllLocateResultsAsync().asLiveData()) { it }
        streamLiveData.observeForever(streamObserve)

        deploymentLiveData = Transformations.map(
            mainRepository.getAllDeploymentLocateResultsAsync().asLiveData()
        ) { it }
        deploymentLiveData.observeForever(deploymentObserve)
    }

    fun fetchProjects() {
        projects.postValue(Resource.loading(null))
        mainRepository.getProjectsFromRemote()
            .enqueue(object : Callback<List<ProjectResponse>> {
                override fun onFailure(call: Call<List<ProjectResponse>>, t: Throwable) {
                    if (!context.isNetworkAvailable()) {
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
            getProjectsFromLocal().filter { project -> project.serverId != null }
        updateProjectBounds.map { projectBounds ->
            val token = "Bearer ${context?.getIdToken()}"
            projectBounds.serverId?.let { serverId ->
                mainRepository.getProjectsByIdFromCore(serverId)
                    .enqueue(object : Callback<ProjectByIdResponse> {
                        override fun onFailure(call: Call<ProjectByIdResponse>, t: Throwable) {}
                        override fun onResponse(
                            call: Call<ProjectByIdResponse>,
                            response: Response<ProjectByIdResponse>
                        ) {
                            if (response.body() != null && response.body()?.id != null && response.body()?.maxLatitude != null) {
                                val res = response.body() as ProjectByIdResponse
                                val project =
                                    res.id?.let { mainRepository.getProjectByServerId(it) }
                                        ?: Project()
                                if (project.minLatitude != res.minLatitude || project.maxLatitude != res.maxLatitude || project.minLongitude != res.minLongitude || project.maxLongitude != res.maxLongitude) {
                                    updateProjectBounds(res)
                                }
                            }
                        }
                    })
            }
        }
    }

    private fun fetchDeletedProjects() {
        mainRepository.getDeletedProjectsFromRemote()
            .enqueue(object : Callback<List<ProjectResponse>> {
                override fun onFailure(call: Call<List<ProjectResponse>>, t: Throwable) {
                    if (!context.isNetworkAvailable()) {
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
                            mainRepository.removeProjectFromLocal(projectsRes) // remove project with these coreIds
                            projects.postValue(Resource.success(null)) // no need to send project data
                        }
                        getProjectOffTimes() // get offtimes after fetch deleted projects
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

    private fun getProjectOffTimes() {
        val projectsLocal =
            getProjectsFromLocal().filter { project -> project.serverId != null }
        projectsLocal.forEach {
            mainRepository.getProjectOffTimeFromRemote(it.serverId!!)
                .enqueue(object : Callback<ProjectOffTimeResponse> {
                    override fun onFailure(call: Call<ProjectOffTimeResponse>, t: Throwable) {
                        if (!context.isNetworkAvailable()) {
                            projects.postValue(
                                Resource.error(
                                    context.getString(R.string.network_not_available),
                                    null
                                )
                            )
                        }
                    }

                    override fun onResponse(
                        call: Call<ProjectOffTimeResponse>,
                        response: Response<ProjectOffTimeResponse>
                    ) {
                        if (response.isSuccessful) {
                            response.body()?.let { offTimesRes ->
                                mainRepository.getProjectLocalDb().updateOffTimeByProjectId(offTimesRes.id, offTimesRes.offTimes)
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
    }

    fun getStreamAssets(site: Stream) {
        tracks.postValue(Resource.loading(null))
        mainRepository.getStreamAssets(site.serverId!!)
            .enqueue(object : Callback<List<DeploymentAssetResponse>> {
                override fun onResponse(
                    call: Call<List<DeploymentAssetResponse>>,
                    response: Response<List<DeploymentAssetResponse>>
                ) {
                    var fileCount = 0
                    var fileCreated = 0
                    var siteAssets = response.body()
                    val assets = siteAssets?.filter { it.mimeType.endsWith("geo+json") }
                    if (!assets.isNullOrEmpty()) {
                        siteAssets = listOf(assets.last())
                    }
                    siteAssets?.forEach { item ->
                        if (item.mimeType.endsWith("geo+json")) {
                            fileCount += 1
                            GeoJsonUtils.downloadGeoJsonFile(
                                context,
                                item,
                                site.serverId!!,
                                Date(),
                                object : GeoJsonUtils.DownloadTrackCallback {
                                    override fun onSuccess(filePath: String) {
                                        fileCreated += 1
                                        mainRepository.saveTrackingToLocal(
                                            item,
                                            filePath,
                                            site
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
        mainRepository.deleteDeploymentWithType(Device.GUARDIAN.value)

        val projectId = getSelectedProjectId()
        val filteredStreams = this.streams.filter { it.project?.id == projectId }
        streamList.postValue(filteredStreams)

        val streams = filteredStreams.filter { it.deployments.isNullOrEmpty() }
        streamMarkers.postValue(streams.map { it.toMark() })

        val deployments =
            filteredStreams.mapNotNull { it.deployments }.flatten().filter { it.isCompleted() }
        val deploymentMarkersList = deployments.map { it.toMark(context) }
        deploymentMarkers.postValue(deploymentMarkersList)

        val unsyncedDeployments = deployments.filter { it.isUnsynced() }
        unsyncedWorksCount.postValue(unsyncedDeployments.size)
    }

    private fun getSelectedProjectId(): Int {
        val preferences = Preferences.getInstance(context)
        val projectId = preferences.getInt(Preferences.SELECTED_PROJECT)
        val project = mainRepository.getProjectById(projectId)
        return project?.id ?: 0
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

    fun getDeploymentMarkers(): LiveData<List<MapMarker.DeploymentMarker>> {
        return deploymentMarkers
    }

    fun getStreamMarkers(): LiveData<List<MapMarker>> {
        return streamMarkers
    }

    fun getUnsyncedWorks(): LiveData<Int> {
        return unsyncedWorksCount
    }

    fun getStreams(): LiveData<List<Stream>> {
        return streamList
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

    fun getDeploymentById(id: Int): Deployment? {
        return mainRepository.getDeploymentById(id)
    }

    fun getStreamById(id: Int): Stream? {
        return mainRepository.getStreamById(id)
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
        streamLiveData.removeObserver(streamObserve)
        deploymentLiveData.removeObserver(deploymentObserve)
    }

    suspend fun shouldBackToLogin(): Boolean {
        val credentialKeeper = CredentialKeeper(context)
        val credentialVerifier = CredentialVerifier(context)
        val refreshToken = Preferences.getInstance(context).getString(Preferences.REFRESH_TOKEN)
        val token = Preferences.getInstance(context).getString(Preferences.ID_TOKEN)

        if (refreshToken == null) {
            return true
        }
        if (token == null) {
            return true
        }
        if (credentialKeeper.hasValidCredentials()) {
            return false
        }

        return suspendCoroutine { cont ->
            authentication.renewAuth(refreshToken).start(object : BaseCallback<Credentials, AuthenticationException> {
                override fun onSuccess(credentials: Credentials?) {
                    if (credentials == null) cont.resume(true)

                    val result = credentialVerifier.verify(credentials!!)
                    when (result) {
                        is Err -> {
                            cont.resume(true)
                        }
                        is Ok -> {
                            val userAuthResponse = result.value
                            credentialKeeper.save(userAuthResponse)
                            cont.resume(false)
                        }
                    }
                }

                override fun onFailure(error: AuthenticationException) {
                    cont.resume(true)
                }
            })
        }
    }
}
