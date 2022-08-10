package org.rfcx.companion.view.profile.guardianregistration

import android.app.Application
import androidx.lifecycle.*
import org.rfcx.companion.entity.guardian.GuardianRegistration
import org.rfcx.companion.service.RegisterGuardianWorker
import org.rfcx.companion.util.asLiveData

class RegisterGuardianViewModel(
    application: Application,
    private val repository: RegisterGuardianRepository
) : AndroidViewModel(application) {

    private lateinit var registrationLiveData: LiveData<List<GuardianRegistration>>
    private val registrationCount = MutableLiveData<List<GuardianRegistration>>()

    private val registrationObserve = Observer<List<GuardianRegistration>> {
        getUnsentCount()
    }

    init {
        fetchLiveData()
    }

    private fun fetchLiveData() {
        registrationLiveData = Transformations.map(
            repository.getAllGuardianRegistrationLocalResultsAsync().asLiveData()
        ) { it }
        registrationLiveData.observeForever(registrationObserve)
    }

    fun registerGuardians() {
        RegisterGuardianWorker.enqueue(getApplication())
    }

    fun getRegistrations(): LiveData<List<GuardianRegistration>> {
        return registrationCount
    }

    fun deleteRegistration(guid: String) {
        repository.deleteRegistration(guid)
    }

    private fun getUnsentCount() {
        registrationCount.postValue(repository.getRegistrations())
    }
}
