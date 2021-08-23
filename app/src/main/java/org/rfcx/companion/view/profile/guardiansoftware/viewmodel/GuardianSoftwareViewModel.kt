package org.rfcx.companion.view.profile.guardiansoftware.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.rfcx.companion.R
import org.rfcx.companion.entity.response.GuardianSoftwareResponse
import org.rfcx.companion.util.Resource
import org.rfcx.companion.util.file.APKUtils
import org.rfcx.companion.util.getIdToken
import org.rfcx.companion.util.isNetworkAvailable
import org.rfcx.companion.view.profile.guardiansoftware.repository.GuardianSoftwareRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GuardianSoftwareViewModel(
    application: Application,
    private val guardianSoftwareRepository: GuardianSoftwareRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private val software = MutableLiveData<Resource<Map<String, APKUtils.APKStatus>>>()

    init {
        checkSoftwareVersion("Bearer ${context.getIdToken()}")
    }

    fun getCurrentDownloadedAPKsVersions(): Map<String, String> {
        return APKUtils.getAllDownloadedSoftwaresVersion(context)
    }

    fun getSoftwareVersion() = software

    private fun checkSoftwareVersion(userToken: String) {
        software.postValue(Resource.loading(null))
        guardianSoftwareRepository.checkSoftwareVersion(userToken)
            .enqueue(object : Callback<List<GuardianSoftwareResponse>> {
                override fun onResponse(
                    call: Call<List<GuardianSoftwareResponse>>,
                    response: Response<List<GuardianSoftwareResponse>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { softwares ->
                            if (!softwares.isNullOrEmpty()) {
                                compareToDownloadedAPKs(softwares)
                            } else {
                                software.postValue(Resource.success(mapOf()))
                            }
                        }
                    } else {
                        software.postValue(
                            Resource.error(
                                context.getString(R.string.something_went_wrong),
                                null
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<List<GuardianSoftwareResponse>>, t: Throwable) {
                    if (context.isNetworkAvailable()) {
                        software.postValue(
                            Resource.error(
                                context.getString(R.string.network_not_available),
                                null
                            )
                        )
                    }
                }
            })
    }

    private fun compareToDownloadedAPKs(softwares: List<GuardianSoftwareResponse>) {
        val roleMappedVersion = APKUtils.getAllDownloadedSoftwaresVersion(context)
        val roleStatus = mutableMapOf<String, APKUtils.APKStatus>()
        softwares.forEach {
            if (roleMappedVersion.keys.contains(it.version)) {
                val isUpToDate = it.version == roleMappedVersion[it.role]
                if (isUpToDate) {
                    roleStatus[it.role] = APKUtils.APKStatus.UP_TO_DATE
                } else {
                    if (APKUtils.compareVersionsIfNeedToUpdate(
                            it.version,
                            roleMappedVersion[it.version]!!
                        )
                    ) {
                        roleStatus[it.role] = APKUtils.APKStatus.NEED_UPDATE
                    } else {
                        roleStatus[it.role] = APKUtils.APKStatus.UP_TO_DATE
                    }
                }
            } else {
                roleStatus[it.role] = APKUtils.APKStatus.NOT_INSTALLED
            }
        }
        software.postValue(Resource.success(roleStatus))
    }

    fun downloadSoftware(url: String) {

    }

}
