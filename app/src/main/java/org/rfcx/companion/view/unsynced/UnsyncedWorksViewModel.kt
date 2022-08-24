package org.rfcx.companion.view.unsynced

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.*
import org.rfcx.companion.R
import org.rfcx.companion.adapter.UnsyncedWorksViewItem
import org.rfcx.companion.entity.RegisterGuardian
import org.rfcx.companion.entity.UnsyncedDeployment
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.guardian.GuardianRegistration
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.service.RegisterGuardianWorker
import org.rfcx.companion.util.asLiveData

class UnsyncedWorksViewModel(
    application: Application,
    private val repository: UnsyncedWorksRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
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
        unsyncedWork.postValue(
            convertToAdapterItem(
                repository.getUnsentDeployment(),
                repository.getUnsentRegistration(),
                deploymentErrors,
                registrationErrors
            )
        )
    }

    private fun convertToAdapterItem(
        deployments: List<Deployment>?,
        registrations: List<GuardianRegistration>?,
        dpErrors: List<UnsyncedDeployment>,
        rgErrors: List<RegisterGuardian>
    ): List<UnsyncedWorksViewItem> {
        val list = mutableListOf<UnsyncedWorksViewItem>()
        if (!deployments.isNullOrEmpty()) {
            list.add(UnsyncedWorksViewItem.Header(context.getString(R.string.deployment)))
            deployments.forEach {
                val dp = dpErrors.find { error -> error.id == it.id }
                list.add(
                    UnsyncedWorksViewItem.Deployment(
                        it.id,
                        it.stream?.name ?: "",
                        it.deployedAt,
                        dp?.error
                    )
                )
            }
        }
        if (!registrations.isNullOrEmpty()) {
            list.add(UnsyncedWorksViewItem.Header(context.getString(R.string.registration)))
            registrations.forEach {
                val rg = rgErrors.find { error -> error.guid == it.guid }
                list.add(UnsyncedWorksViewItem.Registration(it.guid, rg?.error))
            }
        }
        return list
    }
}
