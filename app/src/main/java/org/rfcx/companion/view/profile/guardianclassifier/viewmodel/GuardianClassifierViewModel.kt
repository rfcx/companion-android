package org.rfcx.companion.view.profile.guardianclassifier.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.rfcx.companion.entity.socket.Classifier
import org.rfcx.companion.util.Resource
import org.rfcx.companion.util.getIdToken
import org.rfcx.companion.view.profile.guardianclassifier.repository.GuardianClassifierRepository

class GuardianClassifierViewModel(
    application: Application,
    private val guardianClassifierRepository: GuardianClassifierRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private val availableClassifiers = MutableLiveData<Resource<List<Classifier>>>()

    init {
        checkClassifiers("Bearer ${context.getIdToken()}")
    }

    fun checkClassifiers(userToken: String) {
        availableClassifiers.postValue(Resource.loading(null))
        guardianClassifierRepository.checkClassifiers(userToken)
        //TODO: add check classifiers here
    }
}
