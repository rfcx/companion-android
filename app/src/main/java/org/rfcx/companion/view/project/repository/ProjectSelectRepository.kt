package org.rfcx.companion.view.project.repository

import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class ProjectSelectRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper
) {

    fun getProjectsFromRemote(
        limit: Int = 100,
        offset: Int = 0,
        fields: List<String> = listOf(
            "id",
            "name",
            "isPublic",
            "externalId",
            "permissions"
        )
    ) = deviceApiHelper.getProjects(limit, offset, fields)

    fun getDeletedProjectsFromRemote(
        limit: Int = 100,
        offset: Int = 0,
        onlyDeleted: Boolean = true,
        fields: List<String> = listOf("id")
    ) = deviceApiHelper.getDeletedProjects(limit, offset, onlyDeleted, fields)

    fun getProjectsFromLocal() = localDataHelper.getProjectLocalDb().getProjects()

    fun saveProjectToLocal(projectResponse: ProjectResponse) {
        localDataHelper.getProjectLocalDb().insertOrUpdate(projectResponse)
    }

    fun removeProjectFromLocal(projectResponse: List<ProjectResponse>) {
        localDataHelper.getProjectLocalDb().deleteProjectsByCoreId(projectResponse)
    }
}
