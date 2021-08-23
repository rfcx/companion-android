package org.rfcx.companion.repo.api

class CoreApiHelper(private val coreApiService: CoreApiService) {
    fun checkSoftwareVersion(authUser: String) = coreApiService.checkSoftwareVersion(authUser)
}
