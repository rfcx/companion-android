package org.rfcx.companion.repo.api

// TODO: Convert Old device api to MVVM pattern

class DeviceApiHelper(private val deviceApiService: DeviceApiService) {
    fun getProjects(
        token: String,
        limit: Int = 100,
        offset: Int = 0,
        fields: List<String> = listOf("id", "name", "permissions")
    ) = deviceApiService.getProjects(token, limit, offset, fields)

    fun getProjectsById(token: String, id: String) = deviceApiService.getProjectsById(token, id)

    fun getDeletedProjects(
        token: String,
        limit: Int = 100,
        offset: Int = 0,
        onlyDeleted: Boolean = true,
        fields: List<String> = listOf("id")
    ) = deviceApiService.getDeletedProjects(token, limit, offset, onlyDeleted, fields)

    fun getProjectOffTime(token: String, projectId: String) = deviceApiService.getProjectOffTime(token, projectId)

    fun getStreamAssets(
        token: String,
        id: String
    ) = deviceApiService.getStreamAssets(token, id)

    fun userTouch(token: String) = deviceApiService.userTouch(token)

    fun checkAvailableClassifier(authUser: String) = deviceApiService.checkAvailableClassifiers(authUser)
    fun downloadAPK(url: String) = deviceApiService.downloadFile(url)
}
