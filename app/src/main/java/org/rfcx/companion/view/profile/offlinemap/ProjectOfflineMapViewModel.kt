package org.rfcx.companion.view.profile.offlinemap

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.*
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
        stateOfflineMap.postValue(OfflineMapState.DOWNLOADING_STATE.key)
    }
}
