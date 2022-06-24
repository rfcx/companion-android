package org.rfcx.companion.view.profile.classifier.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.rfcx.companion.R
import org.rfcx.companion.entity.File
import org.rfcx.companion.entity.response.GuardianClassifierResponse
import org.rfcx.companion.util.Resource
import org.rfcx.companion.util.file.FileStatus
import org.rfcx.companion.util.getIdToken
import org.rfcx.companion.util.isNetworkAvailable
import org.rfcx.companion.view.profile.classifier.repository.GuardianClassifierRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GuardianClassifierViewModel(
    application: Application,
    private val guardianClassifierRepository: GuardianClassifierRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    private val availableClassifiers = MutableLiveData<Resource<List<File>>>()

    init {
        checkAvailableClassifiers("Bearer ${context.getIdToken()}")
    }

    fun getDownloadedClassifiersLiveData() = guardianClassifierRepository.getDownloadedClassifierLiveData()
    fun getDownloadedClassifiers() = guardianClassifierRepository.getDownloadedClassifier()
    fun getAvailableClassifiers() = availableClassifiers

    private fun checkAvailableClassifiers(userToken: String) {
        availableClassifiers.postValue(Resource.loading(null))
        guardianClassifierRepository.checkAvailableClassifiers(userToken)
            .enqueue(object : Callback<List<GuardianClassifierResponse>> {
                override fun onResponse(
                    call: Call<List<GuardianClassifierResponse>>,
                    response: Response<List<GuardianClassifierResponse>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { clsf ->
                            if (!clsf.isNullOrEmpty()) {
                                compareToDownloadedClassifiers(clsf)
                            } else {
                                availableClassifiers.postValue(Resource.success(listOf()))
                            }
                        }
                    } else {
                        availableClassifiers.postValue(
                            Resource.error(
                                context.getString(R.string.something_went_wrong),
                                null
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<List<GuardianClassifierResponse>>, t: Throwable) {
                    if (context.isNetworkAvailable()) {
                        availableClassifiers.postValue(
                            Resource.error(
                                context.getString(R.string.network_not_available),
                                null
                            )
                        )
                    }
                }
            })
    }

    private fun compareToDownloadedClassifiers(classifiers: List<GuardianClassifierResponse>) {
        val downloadedClassifiers = guardianClassifierRepository.getDownloadedClassifier() ?: return
        val classifierStatus = mutableListOf<File>()
        classifiers.forEach { res ->
            val downloadedClassifier = downloadedClassifiers.findLast { it.name == res.name }
            if (downloadedClassifier != null) {
                val isUpToDate = res.version.toInt() == downloadedClassifier.version.toInt()
                if (isUpToDate) {
                    classifierStatus.add(File(res, FileStatus.UP_TO_DATE))
                } else {
                    classifierStatus.add(File(res, FileStatus.NEED_UPDATE))
                }
            } else {
                classifierStatus.add(File(res, FileStatus.NOT_DOWNLOADED))
            }
        }
        availableClassifiers.postValue(Resource.success(classifierStatus))
    }
}
