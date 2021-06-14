package org.rfcx.companion.view.project.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.util.Resource
import org.rfcx.companion.util.getIdToken
import org.rfcx.companion.util.isNetworkAvailable
import org.rfcx.companion.view.project.repository.ProjectSelectRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProjectSelectViewModel(
    application: Application,
    private val projectSelectRepository: ProjectSelectRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private val projects = MutableLiveData<Resource<List<Project>>>()

    init {
        fetchProjects()
    }

    private fun fetchProjects() {
        projects.postValue(Resource.loading(null))
        projectSelectRepository.getProjectsFromRemote("Bearer ${context.getIdToken()}")
            .enqueue(object : Callback<List<ProjectResponse>> {
                override fun onFailure(call: Call<List<ProjectResponse>>, t: Throwable) {
                    if (context.isNetworkAvailable()) {
                        projects.postValue(Resource.error("network not available", null))
                    }
                }

                override fun onResponse(
                    call: Call<List<ProjectResponse>>,
                    response: Response<List<ProjectResponse>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { projectsRes ->
                            projectsRes.forEach { item ->
                                projectSelectRepository.saveProjectToLocal(item)
                            }
                            fetchDeletedProjects()
                        }
                    } else {
                        projects.postValue(Resource.error("something went wrong", null))
                    }
                }
            })
    }

    private fun fetchDeletedProjects() {
        projectSelectRepository.getDeletedProjectsFromRemote("Bearer ${context.getIdToken()}")
            .enqueue(object : Callback<List<ProjectResponse>> {
                override fun onFailure(call: Call<List<ProjectResponse>>, t: Throwable) {
                    if (context.isNetworkAvailable()) {
                        projects.postValue(Resource.error("network not available", null))
                    }
                }

                override fun onResponse(
                    call: Call<List<ProjectResponse>>,
                    response: Response<List<ProjectResponse>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { projectsRes ->
                            projectSelectRepository.removeProjectFromLocal(projectsRes.map { it.id!! }) // remove project with these coreIds
                            projects.postValue(Resource.success(null)) // no need to send project data
                        }
                    } else {
                        projects.postValue(Resource.error("something went wrong", null))
                    }
                }
            })
    }

    fun getProjectsFromRemote(): LiveData<Resource<List<Project>>> {
        return projects
    }

    fun getProjectsFromLocal(): List<Project> {
        return projectSelectRepository.getProjectsFromLocal()
    }

    fun refreshProjects() {
        fetchProjects()
    }
}
