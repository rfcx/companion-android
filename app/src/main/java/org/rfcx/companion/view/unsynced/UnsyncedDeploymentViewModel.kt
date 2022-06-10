package org.rfcx.companion.view.unsynced

import android.app.Application
import androidx.lifecycle.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.util.asLiveData

class UnsyncedDeploymentViewModel(
    application: Application,
    private val repository: UnsyncedDeploymentRepository
) : AndroidViewModel(application) {

    private lateinit var deploymentLiveData: LiveData<List<Deployment>>
    private val unsyncedDeploymentCount = MutableLiveData<List<Deployment>>()

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

    fun getUnsyncedDeployments(): LiveData<List<Deployment>> {
        return unsyncedDeploymentCount
    }

    fun deleteDeployment(id: Int) {
        repository.deleteDeployment(id)
    }

    private fun getUnsentCount() {
        unsyncedDeploymentCount.postValue(repository.getUnsentDeployment())
    }
}
