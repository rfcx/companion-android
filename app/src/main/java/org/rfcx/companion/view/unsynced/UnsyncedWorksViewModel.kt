package org.rfcx.companion.view.unsynced

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.*
import org.rfcx.companion.R
import org.rfcx.companion.adapter.UnsyncedWorksViewItem
import org.rfcx.companion.entity.Deployment
import org.rfcx.companion.entity.UnsyncedDeployment
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.util.asLiveData

class UnsyncedWorksViewModel(
    application: Application,
    private val repository: UnsyncedWorksRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private lateinit var deploymentLiveData: LiveData<List<Deployment>>

    private val unsyncedWork = MutableLiveData<List<UnsyncedWorksViewItem>>()

    private val deploymentObserve = Observer<List<Deployment>> {
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
    }

    fun syncDeployment() {
        DeploymentSyncWorker.enqueue(getApplication())
    }

    fun getUnsyncedWorkLiveData(): LiveData<List<UnsyncedWorksViewItem>> {
        return unsyncedWork
    }

    fun deleteDeployment(id: Int) {
        repository.deleteDeployment(id)
    }

    fun updateUnsyncedWorks() {
        val deploymentErrors = DeploymentSyncWorker.getErrors()
        unsyncedWork.postValue(
            convertToAdapterItem(
                repository.getUnsentDeployment(),
                deploymentErrors
            )
        )
    }

    private fun convertToAdapterItem(
        deployments: List<Deployment>?,
        dpErrors: List<UnsyncedDeployment>
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
        return list
    }
}
