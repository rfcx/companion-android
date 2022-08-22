package org.rfcx.companion.view.unsynced

import android.app.Application
import androidx.lifecycle.*
import org.rfcx.companion.adapter.UnsyncedWorksViewItem
import org.rfcx.companion.entity.UnsyncedWork
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.guardian.GuardianRegistration
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.service.RegisterGuardianWorker
import org.rfcx.companion.util.asLiveData

class UnsyncedWorksViewModel(
    application: Application,
    private val repository: UnsyncedWorksRepository
) : AndroidViewModel(application) {

    private lateinit var deploymentLiveData: LiveData<List<Deployment>>

    private lateinit var registrationLiveData: LiveData<List<GuardianRegistration>>

    private val unsyncedWork = MutableLiveData<List<UnsyncedWorksViewItem>>()


    private val deploymentObserve = Observer<List<Deployment>> {
        updateUnsyncedWorks()
    }

    private val registrationObserve = Observer<List<GuardianRegistration>> {
        updateUnsyncedWorks()
    }

    init {
        fetchLiveData()
    }

    private fun fetchLiveData() {
        deploymentLiveData = Transformations.map(
            repository.getAllDeploymentLocalResultsAsync().asLiveData()
        ) { it }
        deploymentLiveData.observeForever(deploymentObserve)

        registrationLiveData = Transformations.map(
            repository.getAllRegistrationLocalResultsAsync().asLiveData()
        ) { it }
        registrationLiveData.observeForever(registrationObserve)
    }

    fun syncDeployment() {
        DeploymentSyncWorker.enqueue(getApplication())
    }

    fun syncRegistration() {
        RegisterGuardianWorker.enqueue(getApplication())
    }

    fun getUnsyncedWorkLiveData(): LiveData<List<UnsyncedWorksViewItem>> {
        return unsyncedWork
    }

    fun deleteDeployment(id: Int) {
        repository.deleteDeployment(id)
    }

    fun deleteRegistration(id: String) {
        repository.deleteRegistration(id)
    }

    fun updateUnsyncedWorks() {
        val deploymentErrors = DeploymentSyncWorker.getErrors()
        val registrationErrors = RegisterGuardianWorker.getErrors()
        unsyncedWork.postValue(UnsyncedWork(repository.getUnsentDeployment(), repository.getUnsetRegistration()).toAdapterItem(deploymentErrors, registrationErrors))
    }
}
