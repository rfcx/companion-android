package org.rfcx.companion

import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.entity.response.DeploymentAssetResponse
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class MainRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper
) {
    fun getProjectsFromRemote(token: String, limit: Int = 100, offset: Int = 0) =
        deviceApiHelper.getProjects(token, limit, offset)

    fun getDeletedProjectsFromRemote(
        token: String,
        limit: Int = 100,
        offset: Int = 0,
        onlyDeleted: Boolean = true,
        fields: List<String> = listOf("id")
    ) = deviceApiHelper.getDeletedProjects(token, limit, offset, onlyDeleted, fields)

    fun getStreamAssets(token: String, id: String) = deviceApiHelper.getStreamAssets(token, id)

    fun getProjectsFromLocal() = localDataHelper.getProjectLocalDb().getProjects()

    fun saveTrackingToLocal(
        deploymentAssetResponse: DeploymentAssetResponse,
        filePath: String,
        deploymentId: Int?
    ) = localDataHelper.getTrackingFileLocalDb()
        .insertOrUpdate(deploymentAssetResponse, filePath, deploymentId)

    fun saveProjectToLocal(projectResponse: ProjectResponse) {
        localDataHelper.getProjectLocalDb().insertOrUpdate(projectResponse)
    }

    fun removeProjectFromLocal(coreIds: List<String>) {
        localDataHelper.getProjectLocalDb().deleteProjectsByCoreId(coreIds)
    }

    fun getProjectById(id: Int): Project? {
        return localDataHelper.getProjectLocalDb().getProjectById(id)
    }

    fun getDeploymentUnsentCount(): Int {
        return localDataHelper.getDeploymentLocalDb().unsentCount().toInt()
    }

    fun getGuardianDeploymentById(id: Int): GuardianDeployment? {
        return localDataHelper.getGuardianDeploymentLocalDb().getDeploymentById(id)
    }

    fun getLocateByName(name: String): Locate? {
        return localDataHelper.getLocateLocalDb().getLocateByName(name)
    }
}
