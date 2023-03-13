package org.rfcx.companion.repo.api

class CoreApiHelper(private val coreApiService: CoreApiService) {
    fun checkSoftwareVersion() = coreApiService.checkSoftwareVersion()
    fun downloadAPK(url: String) = coreApiService.downloadFile(url)
}
