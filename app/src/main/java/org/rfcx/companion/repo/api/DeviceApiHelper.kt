package org.rfcx.companion.repo.api

//TODO: Convert Old device api to MVVM pattern

class DeviceApiHelper(private val deviceApiService: DeviceApiService) {
    fun getProjects(token: String, limit: Int = 100, offset: Int = 0) =
        deviceApiService.getProjects(token, limit, offset)

    fun getDeletedProjects(
        token: String,
        limit: Int = 100,
        offset: Int = 0,
        onlyDeleted: Boolean = true,
        fields: List<String> = listOf("id")
    ) = deviceApiService.getDeletedProjects(token, limit, offset, onlyDeleted, fields)

    fun getStreamAssets(
        token: String,
        id: String
    ) = deviceApiService.getStreamAssets(token, id)
}

