package org.rfcx.companion.repo.api

class CoreApiHelper(private val coreApiService: CoreApiService) {
    fun downloadAPK(url: String) = coreApiService.downloadFile(url)
}
