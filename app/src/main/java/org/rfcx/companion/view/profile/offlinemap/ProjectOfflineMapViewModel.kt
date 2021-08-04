package org.rfcx.companion.view.profile.offlinemap

import android.app.Application
import androidx.lifecycle.*
import org.rfcx.companion.entity.Project
import org.rfcx.companion.util.Resource
import org.rfcx.companion.util.asLiveData

class ProjectOfflineMapViewModel(
    application: Application,
    private val projectOfflineMapRepository: ProjectOfflineMapRepository
) : AndroidViewModel(application) {

    private var projects = MutableLiveData<Resource<List<Project>>>()
    private lateinit var projectsLiveData: LiveData<List<Project>>
    private val projectsObserve = Observer<List<Project>> {
        projects.postValue(Resource.success(it))
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

    fun getProjected(): LiveData<Resource<List<Project>>> {
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

    fun onDestroy() {
        projectsLiveData.removeObserver(projectsObserve)
    }
}
