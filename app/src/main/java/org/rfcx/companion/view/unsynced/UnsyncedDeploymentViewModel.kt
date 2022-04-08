package org.rfcx.companion.view.unsynced

import android.app.Application
import androidx.lifecycle.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.util.Resource
import org.rfcx.companion.util.asLiveData

class UnsyncedDeploymentViewModel(
    application: Application,
    private val repository: UnsyncedDeploymentRepository
) : AndroidViewModel(application) {

    private lateinit var deploymentLiveData: LiveData<List<Deployment>>
    private val unsyncedDeploymentCount = MutableLiveData<Resource<Int>>()

    private val deploymentObserve = Observer<List<Deployment>> {
        getUnsentCount()
    }

    init {
        fetchLiveData()
    }

    private fun fetchLiveData() {
        deploymentLiveData = Transformations.map(
            repository.getAllDeploymentLocalResultsAsync().asLiveData()
        ) { it }
        deploymentLiveData.observeForever(deploymentObserve)
    }

    fun syncDeployment() {
        DeploymentSyncWorker.enqueue(getApplication())
    }

    fun getUnsyncedDeployments(): LiveData<Resource<Int>> {
        return unsyncedDeploymentCount
    }

    private fun getUnsentCount() {
        unsyncedDeploymentCount.postValue(Resource.success(repository.getUnsentDeployment().toInt()))
    }
}
