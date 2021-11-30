package org.rfcx.companion.view.profile.guardiansoftware.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import okhttp3.ResponseBody
import org.rfcx.companion.R
import org.rfcx.companion.entity.APK
import org.rfcx.companion.entity.response.GuardianSoftwareResponse
import org.rfcx.companion.util.Resource
import org.rfcx.companion.util.file.APKUtils
import org.rfcx.companion.util.getIdToken
import org.rfcx.companion.util.insert
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
    private val availableAPKs = MutableLiveData<Resource<Map<String, APK>>>()
    private val downloadAPKs = MutableLiveData<Resource<String>>()
    private var softwareDownloadUrl: Map<String, String>? = null
    private var softwareVersion: Map<String, String>? = null

    init {
        checkSoftwareVersion("Bearer ${context.getIdToken()}")
    }

    fun getCurrentDownloadedAPKsVersions(): Map<String, Pair<String, String>> {
        return APKUtils.getAllDownloadedSoftwaresVersion(context)
    }

    fun getSoftwareVersion() = availableAPKs
    fun getSoftwareFileDownload() = downloadAPKs

    private fun checkSoftwareVersion(userToken: String) {
        availableAPKs.postValue(Resource.loading(null))
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
                                availableAPKs.postValue(Resource.success(mapOf()))
                            }
                        }
                    } else {
                        availableAPKs.postValue(
                            Resource.error(
                                context.getString(R.string.something_went_wrong),
                                null
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<List<GuardianSoftwareResponse>>, t: Throwable) {
                    if (context.isNetworkAvailable()) {
                        availableAPKs.postValue(
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
        extractDownloadUrl(softwares)
        extractVersion(softwares)
        val roleMappedVersion = APKUtils.getAllDownloadedSoftwaresVersion(context)
        val roleStatus = mutableMapOf<String, APK>()
        softwares.forEach { res ->
            if (roleMappedVersion.values.map { it.first }.contains(res.version)) {
                val isUpToDate = res.version == roleMappedVersion[res.role]?.first
                if (isUpToDate) {
                    roleStatus[res.role] = APK(res.role, res.version, APKUtils.APKStatus.UP_TO_DATE)
                } else {
                    if (APKUtils.compareVersionsIfNeedToUpdate(
                            res.version,
                            roleMappedVersion[res.version]?.first
                        )
                    ) {
                        roleStatus[res.role] = APK(res.role, res.version, APKUtils.APKStatus.NEED_UPDATE)
                    } else {
                        roleStatus[res.role] = APK(res.role, res.version, APKUtils.APKStatus.UP_TO_DATE)
                    }
                }
            } else {
                roleStatus[res.role] = APK(res.role, res.version, APKUtils.APKStatus.NOT_INSTALLED)
            }
        }
        availableAPKs.postValue(Resource.success(roleStatus))
    }

    private fun extractDownloadUrl(softwares: List<GuardianSoftwareResponse>) {
        val roleMapToUrl = mutableMapOf<String, String>()
        softwares.forEach {
            roleMapToUrl[it.role] = it.url
        }
        softwareDownloadUrl = roleMapToUrl
    }

    private fun extractVersion(softwares: List<GuardianSoftwareResponse>) {
        val roleMapToVersion = mutableMapOf<String, String>()
        softwares.forEach {
            roleMapToVersion[it.role] = it.version
        }
        softwareVersion = roleMapToVersion
    }

    fun downloadSoftware(role: String) {
        downloadAPKs.postValue(Resource.loading(role))
        softwareDownloadUrl?.let {
            guardianSoftwareRepository.downloadAPK(it[role]!!.insert(4, "s"))
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            response.body()?.let { bytes ->
                                softwareVersion?.let { version ->
                                    val result = APKUtils.apkResponseToDisk(
                                        context,
                                        bytes,
                                        role,
                                        version[role]!!
                                    )
                                    if (result) {
                                        downloadAPKs.postValue(Resource.success(role))
                                    } else {
                                        downloadAPKs.postValue(
                                            Resource.error(
                                                "Cannot build file: $role-${it[role]}.apk.gz",
                                                null
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            downloadAPKs.postValue(
                                Resource.error(
                                    context.getString(R.string.something_went_wrong),
                                    null
                                )
                            )
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        if (context.isNetworkAvailable()) {
                            downloadAPKs.postValue(
                                Resource.error(
                                    context.getString(R.string.network_not_available),
                                    null
                                )
                            )
                        }
                    }

                })
        }
    }

}
