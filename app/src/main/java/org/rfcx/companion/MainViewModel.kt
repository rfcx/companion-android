package org.rfcx.companion

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.realm.RealmResults
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.TrackingFile
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.entity.response.DeploymentAssetResponse
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.service.DownloadStreamsWorker
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.Resource
import org.rfcx.companion.util.geojson.GeoJsonUtils
import org.rfcx.companion.util.getIdToken
import org.rfcx.companion.util.isNetworkAvailable
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

    fun retrieveLocations() {
        val projectId = Preferences.getInstance(context).getInt(Preferences.SELECTED_PROJECT)
        getProjectById(projectId)?.serverId?.let {
            DownloadStreamsWorker.enqueue(context, it)
        }
    }

    fun getProjectsFromRemote(): LiveData<Resource<List<Project>>> {
        return projects
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
}
