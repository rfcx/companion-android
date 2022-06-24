package org.rfcx.companion.repo.api

class CoreApiHelper(private val coreApiService: CoreApiService) {
    fun checkSoftwareVersion(authUser: String) = coreApiService.checkSoftwareVersion(authUser)
    fun checkAvailableClassifier(authUser: String) = coreApiService.checkAvailableClassifiers(authUser)
    fun downloadAPK(url: String) = coreApiService.downloadFile(url)
}
