package org.rfcx.companion.view.project.repository

import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class ProjectSelectRepository(private val deviceApiHelper: DeviceApiHelper, private val localDataHelper: LocalDataHelper) {

    fun getProjectsFromRemote(token: String, limit: Int = 100, offset: Int = 0) =
        deviceApiHelper.getProjects(token, limit, offset)

    fun getDeletedProjectsFromRemote(
        token: String,
        limit: Int = 100,
        offset: Int = 0,
        onlyDeleted: Boolean = true,
        fields: List<String> = listOf("id")
    ) = deviceApiHelper.getDeletedProjects(token, limit, offset, onlyDeleted, fields)

    fun getProjectsFromLocal() = localDataHelper.getProjectLocalDb().getProjects()

    fun saveProjectToLocal(projectResponse: ProjectResponse) {
        localDataHelper.getProjectLocalDb().insertOrUpdate(projectResponse)
    }

    fun removeProjectFromLocal(coreIds: List<String>) {
        localDataHelper.getProjectLocalDb().deleteProjectsByCoreId(coreIds)
    }
}