package org.rfcx.companion

import android.content.Context
import io.realm.RealmResults
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.response.DeploymentAssetResponse
import org.rfcx.companion.entity.response.ProjectByIdResponse
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.localdb.ProjectDb
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class MainRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper
) {
    fun getProjectsFromRemote(
        token: String,
        limit: Int = 100,
        offset: Int = 0,
        fields: List<String> = listOf("id", "name", "permissions")
    ) =
        deviceApiHelper.getProjects(token, limit, offset, fields)

    fun getProjectsByIdFromCore(token: String, id: String) = deviceApiHelper.getProjectsById(token, id)

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

    fun getAllLocateResultsAsync(): RealmResults<Locate> {
        return localDataHelper.getLocateLocalDb().getAllResultsAsync()
    }

    fun getAllDeploymentLocateResultsAsync(): RealmResults<Deployment> {
        return localDataHelper.getDeploymentLocalDb().getAllResultsAsync()
    }

    fun saveProjectToLocal(projectResponse: ProjectResponse) {
        localDataHelper.getProjectLocalDb().insertOrUpdate(projectResponse)
    }

    fun removeProjectFromLocal(coreIds: List<String>) {
        localDataHelper.getProjectLocalDb().deleteProjectsByCoreId(coreIds)
    }

    fun getProjectById(id: Int): Project? {
        return localDataHelper.getProjectLocalDb().getProjectById(id)
    }

    fun getProjectByServerId(id: String): Project? {
        return localDataHelper.getProjectLocalDb().getProjectByServerId(id)
    }

    fun getDeploymentUnsentCount(): Int {
        return localDataHelper.getDeploymentLocalDb().unsentCount().toInt()
    }

    fun getDeploymentById(id: Int): Deployment? {
        return localDataHelper.getDeploymentLocalDb().getDeploymentById(id)
    }

    fun getLocateByName(name: String): Locate? {
        return localDataHelper.getLocateLocalDb().getLocateByName(name)
    }

    fun getLocateById(id: Int): Locate? {
        return localDataHelper.getLocateLocalDb().getLocateById(id)
    }

    fun updateOfflineState(state: String, id: String) {
        localDataHelper.getProjectLocalDb().updateOfflineState(state, id)
    }

    fun updateProjectBounds(response: ProjectByIdResponse) {
        localDataHelper.getProjectLocalDb().updateProjectBounds(response)
    }

    fun getProjectLocalDb(): ProjectDb {
        return localDataHelper.getProjectLocalDb()
    }

    fun getTrackingFileBySiteId(id: Int): RealmResults<TrackingFile> {
        return localDataHelper.getTrackingFileLocalDb().getTrackingFileBySiteId(id)
    }

    fun getFirstTracking(): Tracking? {
        return localDataHelper.getTrackingLocalDb().getFirstTracking()
    }

    fun deleteTracking(id: Int, context: Context) {
        localDataHelper.getTrackingLocalDb().deleteTracking(id, context)
    }
}
