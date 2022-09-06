package org.rfcx.companion.view.profile.classifier.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import okhttp3.ResponseBody
import org.rfcx.companion.R
import org.rfcx.companion.entity.File
import org.rfcx.companion.entity.guardian.Classifier
import org.rfcx.companion.entity.response.GuardianClassifierResponse
import org.rfcx.companion.util.Resource
import org.rfcx.companion.util.file.ClassifierUtils
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
    private val downloadClassifier = MutableLiveData<Resource<Classifier>>()

    private var cacheClassifiers: List<GuardianClassifierResponse>? = null

    init {
        checkAvailableClassifiers("Bearer ${context.getIdToken()}")
    }

    fun getDownloadedClassifiersLiveData() =
        guardianClassifierRepository.getDownloadedClassifierLiveData()

    fun getDownloadClassifier() = downloadClassifier
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
                                cacheClassifiers = clsf
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
                    if (!context.isNetworkAvailable()) {
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

    fun reCompareDownloadedClassifiersWithCacheResponse() {
        cacheClassifiers?.let {
            compareToDownloadedClassifiers(it)
        }
    }

    fun downloadClassifier(classifier: GuardianClassifierResponse) {
        guardianClassifierRepository.downloadClassifier(classifier.path)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { bytes ->
                            val result = ClassifierUtils.classifierResponseToDisk(
                                context,
                                bytes,
                                classifier.id
                            )
                            if (result) {
                                val classifierObj = Classifier(
                                    id = classifier.id,
                                    name = classifier.name,
                                    version = classifier.version,
                                    path = ClassifierUtils.getDownloadedClassifierPath(
                                        context,
                                        classifier.id
                                    ),
                                    type = classifier.type,
                                    sha1 = classifier.sha1,
                                    sampleRate = classifier.sampleRate,
                                    inputGain = classifier.inputGain,
                                    windowSize = classifier.windowSize,
                                    stepSize = classifier.stepSize,
                                    classifications = classifier.classifications,
                                    classificationsFilterThreshold = classifier.classificationsFilterThreshold
                                )
                                downloadClassifier.postValue(Resource.success(classifierObj))
                            } else {
                                downloadClassifier.postValue(
                                    Resource.error(
                                        "Cannot build file: ${classifier.id}.tflite.gz",
                                        null
                                    )
                                )
                            }
                        }
                    } else {
                        downloadClassifier.postValue(
                            Resource.error(
                                context.getString(R.string.something_went_wrong),
                                null
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    if (!context.isNetworkAvailable()) {
                        downloadClassifier.postValue(
                            Resource.error(
                                context.getString(R.string.network_not_available),
                                null
                            )
                        )
                    }
                }
            })
    }

    fun saveClassifier(classifier: Classifier) {
        guardianClassifierRepository.saveClassifier(classifier)
    }

    fun deleteClassifier(id: String) {
        val result = ClassifierUtils.deleteClassifier(context, id)
        if (result) {
            guardianClassifierRepository.deleteClassifier(id)
        }
    }
}
