package org.rfcx.companion.repo.api

// TODO: Convert Old device api to MVVM pattern

class DeviceApiHelper(private val deviceApiService: DeviceApiService) {
    fun getProjects(
        limit: Int = 100,
        offset: Int = 0,
        fields: List<String> = listOf("id", "name", "permissions")
    ) = deviceApiService.getProjects(limit, offset, fields)

    fun getProjectsById(id: String) = deviceApiService.getProjectsById(id)

    fun getDeletedProjects(
        limit: Int = 100,
        offset: Int = 0,
        onlyDeleted: Boolean = true,
        fields: List<String> = listOf("id")
    ) = deviceApiService.getDeletedProjects(limit, offset, onlyDeleted, fields)

    fun getStreamAssets(
        id: String
    ) = deviceApiService.getStreamAssets(id)

    fun userTouch() = deviceApiService.userTouch()

    fun downloadAPK(url: String) = deviceApiService.downloadFile(url)
}
